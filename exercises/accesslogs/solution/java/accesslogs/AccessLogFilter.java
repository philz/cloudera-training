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
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
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
 * AccessLogFilter
 *
 * Reads the access_log entries, parses them, determines if they match a URL (or set
 * of URLs) we are interested in, and emits the ip addresses that accessed that site.
 * Uniquifies URLs in the output.
 *
 * @author(aaron)
 */
public class AccessLogFilter extends Configured implements Tool {

  // where to put the data in hdfs when we're done
  private static final String OUTPUT_PATH = "filtered_ip_addrs";

  // where to read the data from.
  private static final String INPUT_PATH = "/shared/access_logs";


  // log entries related to these files are what we care about.
  private static final String [] interesting_urls = {
    "/random/video/Star_Wars_Kid.wmv",
    "/random/video/Star_Wars_Kid_Remix.wmv"
  };

  /**
   * Reads in the access log line which has the form
   * ip-addr - - [timestamp] "Request str" return_code return_size "referrer url" "user agent"
   * Determines if the request string is for the file we care about, and if so, emits the
   * ip address.
   */
  static class LogFilterMapper extends MapReduceBase
      implements Mapper<LongWritable, Text, Text, Text> {

    public LogFilterMapper() { }

    public void map(LongWritable key, Text value, OutputCollector<Text, Text> output,
        Reporter reporter) throws IOException {

      String accessLine = value.toString();
      AccessLogRecord record = new AccessLogRecord(accessLine);
      String request = record.getRequest();
      String ipAddr = record.getIpAddr();

      if (request != null && ipAddr != null) {
        int finalDotPos = ipAddr.lastIndexOf(".");
        if (finalDotPos != -1) {
          Text classC = new Text(ipAddr.substring(0, finalDotPos));
          Text partD = new Text(ipAddr.substring(finalDotPos + 1));

          for (String url : interesting_urls) {
            if (request.indexOf(url) >= 0) {
              // it's a hit!
              // split the ip addr so that the key gets A.B.C and value gets D.
              output.collect(classC, partD);
            }
          }
        }
      }
    }
  }

  /**
   * The key is a Text "a.b.c"; the value is a Text of the form "d". For each
   * unique D we see, emit the a.b.c once, transformed into a^Ab^Ac.
   */
  static class LogFilterReducer extends MapReduceBase
      implements Reducer<Text, Text, Text, NullWritable> {

    private static final char INPUT_FIELD_SEP = '.';
    private static final char OUTPUT_FIELD_SEP = 1; // ^A

    public void reduce(Text key, Iterator<Text> values,
        OutputCollector<Text, NullWritable> output, Reporter reporter) throws IOException {

      // convert a.b.c into a^Ab^Ac for easier import into Hive.
      String classC = key.toString();
      String asFields = classC.replace(INPUT_FIELD_SEP, OUTPUT_FIELD_SEP);

      Text outKey = new Text(asFields);

      Set<Integer> seenOctets = new HashSet<Integer>();

      while (values.hasNext()) {
        Text val = values.next();
        try {
          Integer lastOctet = new Integer(val.toString());
          if (!seenOctets.contains(lastOctet)) {
            // we have not seen this a.b.c.d before. emit one output entry for
            // the a.b.c, and memorize the d so we don't do this again for the
            // same IP. This is ok to buffer because there will be at most 256
            // unique entries.
            output.collect(outKey, NullWritable.get());
            seenOctets.add(lastOctet);
          }
        } catch (NumberFormatException nfe) {
          // ignore malformed input; just continue.
        }
      }
    }
  }

  /** Driver for the actual MapReduce process */
  private void runJob() throws IOException {

    JobConf conf = new JobConf(getConf(), AccessLogFilter.class);

    FileInputFormat.addInputPath(conf, new Path(INPUT_PATH));
    FileOutputFormat.setOutputPath(conf, new Path(OUTPUT_PATH));

    conf.setMapperClass(LogFilterMapper.class);
    conf.setReducerClass(LogFilterReducer.class);

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(NullWritable.class);

    conf.setMapOutputValueClass(Text.class);

    JobClient.runJob(conf);
  }

  public int run(String [] args) throws IOException {
    runJob();
    return 0;
  }

  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new AccessLogFilter(), args);
    System.exit(ret);
  }
}

