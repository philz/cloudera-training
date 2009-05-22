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
 * TableImports
 *
 * Reads in the cityByCountry table from the ip_to_geo database and creates
 * outputs suitable for loading into two Hive tables  (city id -> city name
 * and country id -> country name)
 *
 * @author(aaron)
 */
public class TableImports extends Configured implements Tool {

  // where to put the data in hdfs when we're done
  private static final String CITY_OUTPUT_PATH = "city_table";
  private static final String COUNTRY_OUTPUT_PATH = "country_table";

  // TODO(aaron): refactor these common constants into a utility class
  //
  // database is IP address --> country lookup.
  private static final String DATABASE_NAME = "ip_to_geo";

  // The JDBC connection URL and driver implementation class
  // TODO(aaron) - This is a specific one-off URL and needs to be a parameter to the program
  private static final String CONNECT_URL =
      "jdbc:mysql://gateway.customerb.my.cloudera.net/" + DATABASE_NAME + "?user=customerb";
  private static final String DATABASE_DRIVER_CLASS = "com.mysql.jdbc.Driver";

  private static final String CITY_INPUT_TABLE = "cityByCountry";
  private static final String COUNTRY_INPUT_TABLE = "countries";

  // how is this data ordered?
  private static final String ORDER_CITY_BY_COL = "country";
  private static final String ORDER_COUNTRY_BY_COL = "id";

  private static final String [] CITY_INPUT_TABLE_FIELDS = {
    "city", "country", "name", "lat", "lng"
  };

  private static final String [] COUNTRY_INPUT_TABLE_FIELDS = {
    "id", "name"
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

    public void readFields(DataInput in) throws IOException {
      this.cityId = in.readLong();
      this.countryId = in.readLong();
      this.name = Text.readString(in);
      this.lat = in.readFloat();
      this.lng = in.readFloat();
    }

    public void readFields(ResultSet resultSet) throws SQLException {
      this.cityId = resultSet.getLong(1);
      this.countryId = resultSet.getLong(2);
      this.name = resultSet.getString(3);
      this.lat = resultSet.getFloat(4);
      this.lng = resultSet.getFloat(5);
    }

    public void write(DataOutput out) throws IOException {
      out.writeLong(this.cityId);
      out.writeLong(this.countryId);
      Text.writeString(out, this.name);
      out.writeFloat(this.lat);
      out.writeFloat(this.lng);
    }

    public void write(PreparedStatement statement) throws SQLException {
      statement.setLong(1, this.cityId);
      statement.setLong(2, this.countryId);
      statement.setString(3, this.name);
      statement.setFloat(4, this.lat);
      statement.setFloat(5, this.lng);
    }
  }

  /**
   * country table members
   */
  static class CountryTableRecord implements Writable, DBWritable {

    long countryId;
    String name;

    public void readFields(DataInput in) throws IOException {
      this.countryId = in.readLong();
      this.name = Text.readString(in);
    }

    public void readFields(ResultSet resultSet) throws SQLException {
      this.countryId = resultSet.getLong(1);
      this.name = resultSet.getString(2);
    }

    public void write(DataOutput out) throws IOException {
      out.writeLong(this.countryId);
      Text.writeString(out, this.name);
    }

    public void write(PreparedStatement statement) throws SQLException {
      statement.setLong(1, this.countryId);
      statement.setString(2, this.name);
    }
  }



  /**
   * Reads in a row from the table and emits the (city id, city name) sub-table
   * separated by ^A characters.
   */
  static class CityNameMapper extends MapReduceBase
      implements Mapper<LongWritable, CityTableRecord, Text, NullWritable> {

    private static final char FIELD_SEPARATOR = 1; // ^A

    public CityNameMapper() { }

    public void map(LongWritable key, CityTableRecord value,
        OutputCollector<Text, NullWritable> output, Reporter reporter) throws IOException {

      StringBuilder sb = new StringBuilder();

      sb.append(Long.toString(value.cityId));
      sb.append(FIELD_SEPARATOR);
      sb.append(value.name);

      output.collect(new Text(sb.toString()), NullWritable.get());
    }
  }

  /**
   * Reads in a row from the table and emits the (city id, city name) sub-table
   * separated by ^A characters.
   */
  static class CountryNameMapper extends MapReduceBase
      implements Mapper<LongWritable, CountryTableRecord, Text, NullWritable> {

    private static final char FIELD_SEPARATOR = 1; // ^A

    public CountryNameMapper() { }

    public void map(LongWritable key, CountryTableRecord value,
        OutputCollector<Text, NullWritable> output, Reporter reporter) throws IOException {

      StringBuilder sb = new StringBuilder();

      sb.append(Long.toString(value.countryId));
      sb.append(FIELD_SEPARATOR);
      sb.append(value.name);

      output.collect(new Text(sb.toString()), NullWritable.get());
    }
  }


  /**
   * load the relevant components of the cityByCountry table
   */
  private void runCityLoadJob() throws IOException {

    JobConf conf = new JobConf(getConf(), TableImports.class);

    conf.setInputFormat(DBInputFormat.class);

    DBConfiguration.configureDB(conf, DATABASE_DRIVER_CLASS, CONNECT_URL);
    DBInputFormat.setInput(conf, CityTableRecord.class, CITY_INPUT_TABLE, null,
      ORDER_CITY_BY_COL, CITY_INPUT_TABLE_FIELDS);

    FileOutputFormat.setOutputPath(conf, new Path(CITY_OUTPUT_PATH));

    conf.setMapperClass(CityNameMapper.class);
    conf.setNumReduceTasks(0);

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(NullWritable.class);

    JobClient.runJob(conf);
  }

  /**
   * load the relevant components of the cityByCountry table
   */
  private void runCountryLoadJob() throws IOException {

    JobConf conf = new JobConf(getConf(), TableImports.class);

    conf.setInputFormat(DBInputFormat.class);

    DBConfiguration.configureDB(conf, DATABASE_DRIVER_CLASS, CONNECT_URL);
    DBInputFormat.setInput(conf, CountryTableRecord.class, COUNTRY_INPUT_TABLE, null,
      ORDER_COUNTRY_BY_COL, COUNTRY_INPUT_TABLE_FIELDS);

    FileOutputFormat.setOutputPath(conf, new Path(COUNTRY_OUTPUT_PATH));

    conf.setMapperClass(CountryNameMapper.class);
    conf.setNumReduceTasks(0);

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(NullWritable.class);

    JobClient.runJob(conf);
  }


  /**
   * Emit the Hive script which loads these outputs into the Hive tables.
   */
  private void createHiveScript() throws IOException {

    FileSystem fs = FileSystem.get(new Configuration());
    Path cityTableDir = new Path(CITY_OUTPUT_PATH).makeQualified(fs);
    Path countryTableDir = new Path(COUNTRY_OUTPUT_PATH).makeQualified(fs);

    // delete the log dirs, or this will confuse the Hive import.
    Path cityTableLogs = new Path(cityTableDir, "_logs");
    Path countryTableLogs = new Path(countryTableDir, "_logs");
    fs.delete(cityTableLogs, true);
    fs.delete(countryTableLogs, true);

    System.out.println("LOAD DATA INPATH '" + cityTableDir.toString()
        + "' INTO TABLE city_names;");
    System.out.println("LOAD DATA INPATH '" + countryTableDir.toString()
        + "' INTO TABLE country_names;");
  }

  /**
   * Main entry point into actual body of this method. Should be called with a ToolRunner:
   * ToolRunner.run(new TableImports(), argv)
   */
  public int run(String [] args) throws IOException {
    runCityLoadJob();
    runCountryLoadJob();
    createHiveScript();
    return 0;
  }

  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new TableImports(), args);
    System.exit(ret);
  }
}

