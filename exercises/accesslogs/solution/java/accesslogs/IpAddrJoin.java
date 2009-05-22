// (c) Copyright 2009 Cloudera, Inc.

package accesslogs;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.apache.hadoop.mapred.lib.db.DBInputFormat;
import org.apache.hadoop.mapred.lib.db.DBWritable;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.util.Tool;

/**
 * IpAddrJoin
 *
 * Reads in the input files from the filtered_ips dataset (all the IP address
 * class C's emitted by the AccessLogFilter) and the ip_locations dataset
 * (all the class C |--> (city, country) records) and joins them.
 *
 * Emits a (city, country) for each IP address that we saw.
 *
 * @author(aaron)
 */
public class IpAddrJoin extends Configured implements Tool {

  // where to put the data in hdfs when we're done
  private static final String OUTPUT_PATH = "joined_ip_addrs";

  // where to read the data from.
  private static final String IP_ADDRESS_INPUTS = "filtered_ip_addrs";
  private static final String LOCATION_DATA_INPUTS = "/user/hive/warehouse/ip_locations";

  private static final char FIELD_SEPARATOR = 1; // ^A is ASCII 1.

  static class JoinRecordKey implements WritableComparable<JoinRecordKey> {
    private int a;
    private int b;
    private int c;

    public JoinRecordKey() {
      this.a = 0;
      this.b = 0;
      this.c = 0;
    }

    public JoinRecordKey(final int a, final int b, final int c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }

    /** Set the a, b, c from the first three integers in the array */
    public JoinRecordKey(final int [] intParts) {
      this.a = intParts[0];
      this.b = intParts[1];
      this.c = intParts[2];
    }

    public int getPartA() {
      return a;
    }

    public int getPartB() {
      return b;
    }

    public int getPartC() {
      return c;
    }

    public void write(DataOutput out) throws IOException {
      out.writeInt(a);
      out.writeInt(b);
      out.writeInt(c);
    }

    public void readFields(DataInput in) throws IOException {
      this.a = in.readInt();
      this.b = in.readInt();
      this.c = in.readInt();
    }

    public int compareTo(JoinRecordKey other) {
      if (null == other) {
        return 1;
      }

      if (this.a == other.a) {
        if (this.b == other.b) {
          if (this.c == other.c) {
            return 0;
          } else if (this.c > other.c) {
            return 1;
          } else {
            return -1;
          }
        } else if (this.b > other.b) {
          return 1;
        } else {
          return -1;
        }
      } else if (this.a > other.a) {
        return 1;
      } else {
        return -1;
      }
    }

    public boolean equals(Object other) {
      if (other != null && other instanceof JoinRecordKey) {
        JoinRecordKey key = (JoinRecordKey) other;
        return a == key.a && b == key.b && c == key.c;
      } else {
        return false;
      }
    }

    public int hashCode() {
      return a ^ b ^ c;
    }
  }

  static class JoinRecordValue implements Writable {
    private static final int INVALID_ID = -1;

    private int cityId;
    private int countryId;

    public JoinRecordValue() {
      this.cityId = INVALID_ID;
      this.countryId = INVALID_ID;
    }

    public JoinRecordValue(final int city, final int country) {
      this.cityId = city;
      this.countryId = country;
    }

    public void write(DataOutput out) throws IOException {
      out.writeInt(cityId);
      out.writeInt(countryId);
    }

    public void readFields(DataInput in) throws IOException {
      this.cityId = in.readInt();
      this.countryId = in.readInt();
    }

    public int getCityId() {
      return this.cityId;
    }

    public int getCountryId() {
      return this.countryId;
    }

    // singleton instance for an invalid entry.
    private static JoinRecordValue logEntryVal;

    /** Return the singleton instance of JoinRecordValue identifying an invalid
     * entry (Which means that it is a placeholder value for an IP hit against
     * the class C in the key.
     */
    public static JoinRecordValue getLogEntryInstance() {
      if (null == logEntryVal) {
        // instantiate singleton.
        logEntryVal = new JoinRecordValue(INVALID_ID, INVALID_ID);
      }

      return logEntryVal;
    }

    /**
     * @return true if this is a log entry-style value, false if it's the actual (city, country)
     * for this class C
     */
    public boolean isLogEntry() {
      return cityId == INVALID_ID && countryId == INVALID_ID;
    }
  }

  /**
   * Reads in and discriminates between two types of input records.
   *
   * Filtered IP addresses are a 3-tuple separated by ^A characters
   * IP address to city resolutions are a 5-tuple separated by ^A characters.
   *
   * Reads the record, finds the type, and emits as output a JoinRecordKey
   * containing the class C, and a JoinRecordValue
   * which is either a two-tuple of ints describing a (city, state) listing
   * or the special two-tuple (-1, -1) indicating an instance of an IP address
   * hit from the access_log.
   */
  static class IpAddrJoinMapper extends MapReduceBase
      implements Mapper<LongWritable, Text, JoinRecordKey, JoinRecordValue> {

    // expected number of components for each of the different record types
    private static final int MINIMUM_PARTS = 3;
    private static final int IP_ONLY_LENGTH = 3;
    private static final int CITY_ID_LENGTH = 5;

    public IpAddrJoinMapper() { }

    /**
     * Given an array of strings, each of which contains a base-10 encoded
     * integer, return the array of integers that it corresponds to.
     */
    private int [] getInts(String [] strings) throws NumberFormatException {
      if (null == strings) {
        return null;
      }

      int [] ints = new int[strings.length];
      for (int i = 0; i < strings.length; i++) {
        // NOTE(aaron): For posterity -- if you emit a string as the key,
        // and no string as the value, it'll still put the '\t' after the key.
        ints[i] = Integer.valueOf(strings[i].trim());
      }

      return ints;
    }

    public void map(LongWritable key, Text value,
        OutputCollector<JoinRecordKey, JoinRecordValue> output, Reporter reporter)
        throws IOException {

      // create a string from the 1-character field separator;
      // since this is a ^A, it should be a regex for itself.
      char [] fieldSepChars = new char[1];
      fieldSepChars[0] = FIELD_SEPARATOR;
      String [] parts = value.toString().split(new String(fieldSepChars));

      if (parts.length >= MINIMUM_PARTS) {
        try {
          // get the int components on this line.
          int [] asInts = getInts(parts);

          if (parts.length == IP_ONLY_LENGTH) {
            // it's just an IP address
            output.collect(new JoinRecordKey(asInts),
                JoinRecordValue.getLogEntryInstance());
          } else if (parts.length == CITY_ID_LENGTH) {
            // it's an ip address and also the (city, country) ids
            output.collect(new JoinRecordKey(asInts),
                new JoinRecordValue(asInts[3], asInts[4]));
          }
        } catch (NumberFormatException nfe) {
          // unparsible line. do nothing, since we skipped
          // the uses of asInts by throwing to here.
        }
      }
    }
  }

  /**
   * The key is a Text "a.b.c"; the value is a Text of the form "d". For each
   * unique D we see, emit the a.b.c once, transformed into a^Ab^Ac.
   */
  static class IpAddrJoinReducer extends MapReduceBase
      implements Reducer<JoinRecordKey, JoinRecordValue, Text, Text> {

    public void reduce(JoinRecordKey key, Iterator<JoinRecordValue> values,
        OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

      int numInstances = 0;
      int cityId = 0;
      int countryId = 0;

      while (values.hasNext()) {
        JoinRecordValue val = values.next();

        if (!val.isLogEntry()) {
          // grab the cityId and countryId from this;
          cityId = val.getCityId();
          countryId = val.getCountryId();
        } else {
          // this is an access_log entry. since they're all the same
          // within the same key, just increment a counter.
          numInstances++;
        }
      }

      // now that all these are examined, just emit the (city, country)
      StringBuffer sb = new StringBuffer();
      sb.append(Integer.toString(cityId));
      sb.append(FIELD_SEPARATOR);
      sb.append(Integer.toString(countryId));

      for (int i = 0; i < numInstances; i++) {
        output.collect(new Text(sb.toString()), new Text(""));
      }
    }
  }

  /** Driver for the actual MapReduce process */
  private void runJob() throws IOException {
    JobConf conf = new JobConf(getConf(), IpAddrJoin.class);

    FileInputFormat.addInputPath(conf, new Path(IP_ADDRESS_INPUTS));
    FileInputFormat.addInputPath(conf, new Path(LOCATION_DATA_INPUTS));
    FileOutputFormat.setOutputPath(conf, new Path(OUTPUT_PATH));

    conf.setMapperClass(IpAddrJoinMapper.class);
    conf.setReducerClass(IpAddrJoinReducer.class);

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(Text.class);

    conf.setMapOutputKeyClass(JoinRecordKey.class);
    conf.setMapOutputValueClass(JoinRecordValue.class);

    conf.setNumReduceTasks(5);

    JobClient.runJob(conf);
  }

  public int run(String [] args) throws IOException {
    runJob();
    return 0;
  }

  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new IpAddrJoin(), args);
    System.exit(ret);
  }
}

