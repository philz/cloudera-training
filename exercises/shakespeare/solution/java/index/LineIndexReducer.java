// (c) Copyright 2009 Cloudera, Inc.

package index;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.JobConf;

/**
 * LineIndexReducer
 *
 * Takes a list of filename@offset entries for a single word and concatenates
 * them into a list.
 *
 * @author(aaron)
 */
public class LineIndexReducer extends MapReduceBase
    implements Reducer<Text, Text, Text, Text> {

  private static final String SEP = ",";

  public LineIndexReducer() { }

  public void reduce(Text key, Iterator<Text> values,
      OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

    StringBuilder sb = new StringBuilder();
    boolean first = true;

    while (values.hasNext()) {
      if (!first) {
        sb.append(SEP);
      }

      sb.append(values.next().toString());
      first = false;
    }

    output.collect(key, new Text(sb.toString()));
  }
}

