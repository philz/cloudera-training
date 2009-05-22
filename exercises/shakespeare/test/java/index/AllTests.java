// (c) Copyright 2009 Cloudera, Inc.

package index;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All tests for inverted index code
 *
 * @author aaron
 */
public final class AllTests  {

  private AllTests() { }

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for inverted index");

    suite.addTestSuite(MapperTest.class);
    suite.addTestSuite(ReducerTest.class);

    return suite;
  }

}

