// (c) Copyright 2009 Cloudera, Inc.

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All tests for the stub code for the training workshop
 *
 * @author aaron
 */
public final class AllTests  {

  private AllTests() { }

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for training workshop stubs");
    suite.addTestSuite(AccessLogRecordTest.class);
    return suite;
  }

}

