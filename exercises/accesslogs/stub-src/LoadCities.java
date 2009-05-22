// (c) Copyright 2009 Cloudera, Inc.

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
 * LoadCities
 *
 * Reads in the cityByCountry table from the ip_to_geo database and creates
 * outputs suitable for loading into the Hive table for cities.
 */
public class LoadCities extends Configured implements Tool {

  // what table of the database do we read from?
  private static final String CITY_INPUT_TABLE = "cityByCountry";

  // where to put the data in hdfs when we're done
  private static final String CITY_OUTPUT_PATH = "city_table";

  // what mysql database do we connect to
  private static final String DATABASE_NAME = "ip_to_geo";

  // The JDBC connection URL and driver implementation class
  private static final String CONNECT_URL =
      "jdbc:mysql://gateway.customerb.my.cloudera.net/" + DATABASE_NAME + "?user=customerb";

  private static final String DATABASE_DRIVER_CLASS = "com.mysql.jdbc.Driver";

  // how is this data ordered?
  private static final String ORDER_CITY_BY_COL = "country";

  private static final String [] CITY_INPUT_TABLE_FIELDS = {
    "city", "country", "name", "lat", "lng"
  };

  /**
   * cityByCountry table members
   */
  static class CityTableRecord implements Writable, DBWritable {

    long cityId;
    long countryId;
    String name;
    float lat;
    float lng;

    /**
     * Reads data from other in-Hadoop stages of the MapReduce
     * pipeline. readFields(DataInput) must read the fields in
     * the same order as write(DataOutput) writes them. It must
     * use the same types.
     *
     * See java.io.DataInput for methods.
     * Use 'public static String Text.readString(DataInput in)' to
     * read string data.
     */
    public void readFields(DataInput in) throws IOException {
      // For this training example, this does not need to be implemented.
    }

    /**
     * Reads data from the SQL/JDBC connector. Must read the fields
     * in the same order as they are enumerated in the
     * CITY_INPUT_TABLE_FIELDS array. Offsets into the resultSet are
     * 1-based. (e.g., resultSet.getLong(1) returns the first field.)
     *
     * See java.sql.ResultSet() for members.
     */
    public void readFields(ResultSet resultSet) throws SQLException {
      // TODO: Write this.
    }

    /**
     * Uses java.io.DataOutput to write the elements of this object
     * to subsequent in-Hadoop stages of the pipeline. Must write
     * fields in the same order (and with the same serialization types)
     * as the readFields(DataInput) method. Use Text.writeString() to
     * write string-based data.
     */
    public void write(DataOutput out) throws IOException {
      // For this training example, this does not need to be implemented.
    }

    /**
     * Used only when writing data back to a database. Can be left blank.
     */
    public void write(PreparedStatement statement) throws SQLException {
      // For this training example, this does not need to be implemented.
    }
  }

  /**
   * Reads in a row from the table and emits the city id and city name columns
   * as a single Text object separated by ^A (ASCII value 1) characters.
   */
  static class CityNameMapper extends MapReduceBase
      implements Mapper<LongWritable, CityTableRecord, Text, NullWritable> {

    public CityNameMapper() { }

    public void map(LongWritable key, CityTableRecord value,
        OutputCollector<Text, NullWritable> output, Reporter reporter) throws IOException {
      // TODO: Write your mapper logic here.
    }
  }

  /**
   * Main entry point into actual body of this class.
   * Should be called with a ToolRunner:
   * ToolRunner.run(new TableImports(), argv)
   *
   * TODO: Your main logic goes here.
   * As is written, this sets up a MapReduce job to read from the
   * correct MySQL table and write its output to an HDFS path.
   */
  public int run(String [] args) throws IOException {
    JobConf conf = new JobConf(getConf(), LoadCities.class);

    conf.setInputFormat(DBInputFormat.class);

    DBConfiguration.configureDB(conf, DATABASE_DRIVER_CLASS, CONNECT_URL);
    DBInputFormat.setInput(conf, CityTableRecord.class, CITY_INPUT_TABLE, null,
      ORDER_CITY_BY_COL, CITY_INPUT_TABLE_FIELDS);

    FileOutputFormat.setOutputPath(conf, new Path(CITY_OUTPUT_PATH));

    conf.setMapperClass(CityNameMapper.class);
    conf.setNumReduceTasks(0); // does not require a reducer

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(NullWritable.class);

    JobClient.runJob(conf);
    return 0;
  }

  /**
   * boilerplate main() method to run a MapReduce job.
   */
  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new LoadCities(), args);
    System.exit(ret);
  }
}

