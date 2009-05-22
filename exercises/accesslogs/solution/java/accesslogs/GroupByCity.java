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
import org.apache.hadoop.mapred.lib.LongSumReducer;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.apache.hadoop.mapred.lib.db.DBInputFormat;
import org.apache.hadoop.mapred.lib.db.DBWritable;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.util.Tool;

/**
 * GroupByCity
 *
 * After IpAddrJoin, we have several (city, country) pairs, one for each
 * IP address we saw. The mapper emits ((city, country), 1) for each of
 * these and the reducer just sums up the 1's into an 'n', and emits
 * city^Acountry^Acount
 *
 * @author(aaron)
 */
public class GroupByCity extends Configured implements Tool {

  // where to put the data in hdfs when we're done
  private static final String OUTPUT_PATH = "grouped_ip_addrs";

  // where to read the data from.
  private static final String INPUT_PATH = "joined_ip_addrs";

  private static final char FIELD_SEPARATOR = 1; // ^A is ASCII 1.

  /**
   * For each input line of text, emit (line, 1)
   */
  static class GroupByCityMapper extends MapReduceBase
      implements Mapper<LongWritable, Text, Text, LongWritable> {

    public GroupByCityMapper() { }

    private static LongWritable one = new LongWritable(1);

    public void map(LongWritable key, Text value,
        OutputCollector<Text, LongWritable> output, Reporter reporter)
        throws IOException {

      output.collect(value, one);
    }
  }

  /**
   * The key is a Text "a.b.c"; the value is a Text of the form "d". For each
   * unique D we see, emit the a.b.c once, transformed into a^Ab^Ac.
   */
  static class GroupByCityReducer extends MapReduceBase
      implements Reducer<Text, LongWritable, Text, Text> {

    public void reduce(Text key, Iterator<LongWritable> values,
        OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

      long count = 0;

      while (values.hasNext()) {
        count += values.next().get();
      }

      // emit this as a string record delimited by ^A's
      StringBuffer sb = new StringBuffer();
      sb.append(key.toString().trim());
      sb.append(FIELD_SEPARATOR);
      sb.append(Long.toString(count));

      output.collect(new Text(sb.toString()), new Text(""));
    }
  }

  /** Driver for the actual MapReduce process */
  private void runJob() throws IOException {
    JobConf conf = new JobConf(getConf(), GroupByCity.class);

    FileInputFormat.addInputPath(conf, new Path(INPUT_PATH));
    FileOutputFormat.setOutputPath(conf, new Path(OUTPUT_PATH));

    conf.setMapperClass(GroupByCityMapper.class);
    conf.setCombinerClass(LongSumReducer.class);
    conf.setReducerClass(GroupByCityReducer.class);

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(Text.class);

    conf.setMapOutputKeyClass(Text.class);
    conf.setMapOutputValueClass(LongWritable.class);

    conf.setNumReduceTasks(5);

    JobClient.runJob(conf);
  }

  public int run(String [] args) throws IOException {
    runJob();
    return 0;
  }

  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new GroupByCity(), args);
    System.exit(ret);
  }
}

