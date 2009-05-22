// (c) Copyright 2009 Cloudera, Inc.

package index;

import static org.apache.hadoop.mrunit.testutil.ExtendedAssert.*;

import org.apache.hadoop.mrunit.ReduceDriver;
import org.apache.hadoop.mrunit.types.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reducer;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the inverted index reducer.
 */
public class ReducerTest extends TestCase {

  private Reducer<Text, Text, Text, Text> reducer;
  private ReduceDriver<Text, Text, Text, Text> driver;

  @Before
  public void setUp() {
    reducer = new LineIndexReducer();
    driver = new ReduceDriver<Text, Text, Text, Text>(reducer);
  }

  @Test
  public void testOneOffset() {
    List<Pair<Text, Text>> out = null;

    try {
      out = driver.withInputKey(new Text("word"))
                  .withInputValue(new Text("offset"))
                  .run();
    } catch (IOException ioe) {
      fail();
    }

    List<Pair<Text, Text>> expected = new ArrayList<Pair<Text, Text>>();
    expected.add(new Pair<Text, Text>(new Text("word"), new Text("offset")));

    assertListEquals(expected, out);
  }

  @Test
  public void testMultiOffset() {
    List<Pair<Text, Text>> out = null;

    try {
      out = driver.withInputKey(new Text("word"))
                  .withInputValue(new Text("offset1"))
                  .withInputValue(new Text("offset2"))
                  .run();
    } catch (IOException ioe) {
      fail();
    }

    List<Pair<Text, Text>> expected = new ArrayList<Pair<Text, Text>>();
    expected.add(new Pair<Text, Text>(new Text("word"), new Text("offset1,offset2")));

    assertListEquals(expected, out);
  }

}

