// (c) Copyright 2009 Cloudera, Inc.

package accesslogs;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.apache.hadoop.mapred.lib.db.DBInputFormat;
import org.apache.hadoop.mapred.lib.db.DBWritable;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.util.Tool;

/**
 * IpAddrImport
 *
 * Reads the IP-address lookup tables from the ip_to_geo database into a set of files
 * in HDFS, then moves the files into the same directory.
 *
 * @author(aaron)
 */
public class IpAddrImport extends Configured implements Tool {

  // where to put the data in hdfs when we're done
  // this should not end with a '/'
  private static final String OUTPUT_PATH = "ip_addr_tables";

  // where do we put the merged results (see createHiveScript()?
  private static final String MERGED_DIR_NAME = "merged";
  private static final String FINAL_OUTPUT_PATH = OUTPUT_PATH + "/" + MERGED_DIR_NAME;

  // database is IP address --> country lookup.
  private static final String DATABASE_NAME = "ip_to_geo";

  // The JDBC connection URL and driver implementation class
  // TODO(aaron) - This is a specific one-off URL and needs to be a parameter to the program
  private static final String CONNECT_URL =
      "jdbc:mysql://localhost/" + DATABASE_NAME + "?user=training";
  private static final String DATABASE_DRIVER_CLASS = "com.mysql.jdbc.Driver";

  // where we read the data from
  private static final String INPUT_TABLE_PREFIX = "ip4_";
  private static final String [] INPUT_TABLE_FIELDS = {
    "b", "c", "city", "country"
  };

  // how many tables are there to read?
  private static final int NUM_TABLES = 256;

  // how is this data ordered?
  private static final String ORDER_BY_COL = "country";

  // what table id are we reading from? The tables are all named "ip4_N"
  // for N in 0..255. This config parameter records 'N'.
  private static final String TABLE_NUM_KEY = "import.table.num";

  // name of the table in Hive where the data should end up.
  private static final String HIVE_TABLE_NAME = "ip_locations";

  static class IpTableRecord implements Writable, DBWritable {
    long field_b;
    long field_c;
    long cityId;
    long countryId;

    public void readFields(DataInput in) throws IOException {
      this.field_b = in.readLong();
      this.field_c = in.readLong();
      this.cityId = in.readLong();
      this.countryId = in.readLong();
    }

    public void readFields(ResultSet resultSet) throws SQLException {
      this.field_b = resultSet.getLong(1);
      this.field_c = resultSet.getLong(2);
      this.cityId = resultSet.getLong(3);
      this.countryId = resultSet.getLong(4);
    }

    public void write(DataOutput out) throws IOException {
      out.writeLong(this.field_b);
      out.writeLong(this.field_c);
      out.writeLong(this.cityId);
      out.writeLong(this.countryId);
    }

    public void write(PreparedStatement statement) throws SQLException {
      statement.setLong(1, this.field_b);
      statement.setLong(2, this.field_c);
      statement.setLong(3, this.cityId);
      statement.setLong(4, this.countryId);
    }
  }

  /**
   * Reads in a row from the table and emits a text description of the row with fields
   * separated by ^A characters. Also inserts the first part of the dotted-quad into the
   * output record as taken from the table name (passed in as a separate configuration param)
   */
  static class IpAddrTableMapper extends MapReduceBase
      implements Mapper<LongWritable, IpTableRecord, Text, NullWritable> {


    private static final char FIELD_SEPARATOR = 1; // ^A

    private static final int INVALID_TABLE_NUM = -1;

    int tableNum;

    public IpAddrTableMapper() { }

    public void configure(JobConf conf) {
      tableNum = conf.getInt(TABLE_NUM_KEY, INVALID_TABLE_NUM);
      if (tableNum == INVALID_TABLE_NUM) {
        throw new RuntimeException("Did not set property " + TABLE_NUM_KEY);
      }
    }

    public void map(LongWritable key, IpTableRecord value,
        OutputCollector<Text, NullWritable> output, Reporter reporter) throws IOException {

      StringBuilder sb = new StringBuilder();

      sb.append(Integer.toString(tableNum));
      sb.append(FIELD_SEPARATOR);
      sb.append(Long.toString(value.field_b));
      sb.append(FIELD_SEPARATOR);
      sb.append(Long.toString(value.field_c));
      sb.append(FIELD_SEPARATOR);
      sb.append(Long.toString(value.cityId));
      sb.append(FIELD_SEPARATOR);
      sb.append(Long.toString(value.countryId));

      output.collect(new Text(sb.toString()), NullWritable.get());
    }
  }

  /** Driver for the actual MapReduce process */
  private void runJob(int tableNum) throws IOException {

    JobConf conf = new JobConf(getConf(), IpAddrImport.class);

    // determine the actual name of the table we're going to read
    String inputTableName = INPUT_TABLE_PREFIX + Integer.toString(tableNum);
    conf.setInt(TABLE_NUM_KEY, tableNum);

    // determine what hdfs dir the output goes in.
    String tableOutputPath = OUTPUT_PATH + "/" + Integer.toString(tableNum);

    conf.setInputFormat(DBInputFormat.class);

    DBConfiguration.configureDB(conf, DATABASE_DRIVER_CLASS, CONNECT_URL);
    DBInputFormat.setInput(conf, IpTableRecord.class, inputTableName, null,
      ORDER_BY_COL, INPUT_TABLE_FIELDS);

    FileOutputFormat.setOutputPath(conf, new Path(tableOutputPath));

    conf.setMapperClass(IpAddrTableMapper.class);
    conf.setNumReduceTasks(0);

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(NullWritable.class);

    JobClient.runJob(conf);
  }

  /**
   * Each of the mapreduce passes has created its output in a directory
   * named ip_addr_tables/$n where $n is in 0..255.
   *
   * Hive expects all files that will compose a table to have separate names.
   * So we move all the files into a single directory and alpha-rename them.
   *
   * Then we emit as local output a script which contains the hive commands
   * to execute to load all of the files into Hive.
   */
  private void createHiveScript() throws IOException {
    int fileNum = 0;
    FileSystem fs = FileSystem.get(new Configuration());
    Path finalPath = new Path(FINAL_OUTPUT_PATH).makeQualified(fs);

    if (fs.exists(finalPath)) {
      System.err.println("Final output path " + FINAL_OUTPUT_PATH + " already exists.");
      return; // refuse to do anything else to this directory
    }

    // create the final output directory.
    fs.mkdirs(new Path(FINAL_OUTPUT_PATH), null);

    // walk through the input directories and move their files into the merged dir.
    Path basePath = new Path(OUTPUT_PATH);
    FileStatus [] statuses = fs.listStatus(basePath);
    for (FileStatus subdir : statuses) {
      Path shardPath = subdir.getPath();
      if (subdir.isDir() && !subdir.getPath().getName().equals(MERGED_DIR_NAME)) {
        // we've found a $n subdir.
        String shardName = shardPath.getName(); // determine the value of $n
        FileStatus [] partStatuses = fs.listStatus(shardPath);
        for (FileStatus part : partStatuses) {
          if (!part.isDir()) {
            // move this file into the merged dir, and emit the load script entry for it.
            Path destPath = new Path(finalPath, Integer.toString(fileNum++));
            fs.rename(part.getPath(), destPath);
            System.out.println("LOAD DATA INPATH '" + destPath.toString()
                + "' INTO TABLE " + HIVE_TABLE_NAME + ";");
          }
        }
      }
    }
  }

  /**
   * Main entry point into actual body of this method. Should be called with a ToolRunner:
   * ToolRunner.run(new IpAddrImport(), argv)
   */
  public int run(String [] args) throws IOException {
    int startTable = 0;

    if (args.length > 0) {
      try {
        startTable = Integer.parseInt(args[0]);
      } catch (NumberFormatException nfe) {
        throw new RuntimeException("Invalid argument to IpAddrImport: " + args[0]);
      }
    }

    for (int i = startTable; i < NUM_TABLES; i++) {
      System.err.println("Reading table: " + i);
      runJob(i);
    }

    createHiveScript();
    return 0;
  }

  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new IpAddrImport(), args);
    System.exit(ret);
  }
}

