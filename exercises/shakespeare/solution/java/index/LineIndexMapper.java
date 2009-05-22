// (c) Copyright 2009 Cloudera, Inc.

package index;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.JobConf;

/**
 * LineIndexMapper
 *
 * Maps each observed word in a line to a (filename@offset) string.
 *
 * @author(aaron)
 */
public class LineIndexMapper extends MapReduceBase
    implements Mapper<LongWritable, Text, Text, Text> {

  public LineIndexMapper() { }

  public void map(LongWritable key, Text value, OutputCollector<Text, Text> output,
      Reporter reporter) throws IOException {

    FileSplit fileSplit = (FileSplit)reporter.getInputSplit();
    String fileName = fileSplit.getPath().getName();
    Text outVal = new Text(fileName + "@" + key.toString());

    StringTokenizer tokenizer = new StringTokenizer(value.toString());
    while (tokenizer.hasMoreTokens()) {
      String word = tokenizer.nextToken();
      output.collect(new Text(word), outVal);
    }
  }
}

