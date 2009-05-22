// (c) Copyright 2009 Cloudera, Inc.

package index;

import java.io.IOException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.util.Tool;

/**
 * StreamingSetup
 *
 * Reformat the input corpus to embed original filenames into the output key
 * along with the offset so that it can be used by Hadoop Streaming.
 *
 * @author(aaron)
 */
public class StreamingSetup extends Configured implements Tool {

  // where to put the data in hdfs when we're done
  private static final String OUTPUT_PATH = "output";

  // where to read the data from.
  private static final String INPUT_PATH = "input";

  static class StreamingSetupMapper extends MapReduceBase
      implements Mapper<LongWritable, Text, Text, Text> {

    public StreamingSetupMapper() { }

    public void map(LongWritable key, Text value, OutputCollector<Text, Text> output,
        Reporter reporter) throws IOException {

      FileSplit fileSplit = (FileSplit)reporter.getInputSplit();
      String fileName = fileSplit.getPath().getName();
      Text outKey = new Text(fileName + "@" + key.toString());

      output.collect(outKey, value);
    }
  }


  /** Driver for the actual MapReduce process */
  private void runJob() throws IOException {
    JobConf conf = new JobConf(getConf(), StreamingSetup.class);

    FileInputFormat.addInputPath(conf, new Path(INPUT_PATH));
    FileOutputFormat.setOutputPath(conf, new Path(OUTPUT_PATH));

    conf.setMapperClass(StreamingSetupMapper.class);
    conf.setNumReduceTasks(0);

    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(Text.class);

    JobClient.runJob(conf);
  }

  public int run(String [] args) throws IOException {
    runJob();
    return 0;
  }

  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new StreamingSetup(), args);
    System.exit(ret);
  }
}

