// (c) Copyright 2009 Cloudera, Inc.

import junit.framework.TestCase;

import org.junit.Test;

/**
 * @author aaron
 */
public class AccessLogRecordTest extends TestCase {

  // line from an actual access_log to test against.
  private static final String LOG_ENTRY = "62.172.72.131 - - [02/Jan/2003:02:06:41 -0700] "
      + "\"GET /random/html/riaa_hacked/ HTTP/1.0\" 200 10564 \"-\" "
      + "\"Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 4.0; WWP 17 August 2001)\"";

  private static final String RANDOM_CHARACTERS = "ASDFA SAD  @# as asdj 1;j \"  \" asd l  dsa ";

  // stripQuotes() utility method.

  /** "foo" -> foo */
  @Test
  public void testStripQuotes() {
    assertEquals(AccessLogRecord.stripQuotes("\"foo\""), "foo");
  }

  /** foo -> foo */
  @Test
  public void testStripQuotesWithNone() {
    assertEquals(AccessLogRecord.stripQuotes("foo"), "foo");
  }

  /** \"foo -> foo */
  @Test
  public void testStripQuotesLeft() {
    assertEquals(AccessLogRecord.stripQuotes("\"foo"), "foo");
  }

  /** foo\"-> foo */
  @Test
  public void testStripQuotesRight() {
    assertEquals(AccessLogRecord.stripQuotes("foo\""), "foo");
  }


  // the actual parser

  /** ok with an empty string, just don't do aynthing (but don't crash) */
  @Test
  public void testParseNothing() {
    AccessLogRecord record = new AccessLogRecord("");
    assertEquals(record.getRecord(), "");
  }

  /** ok with null input; do nothing. */
  @Test
  public void testParseNull() {
    AccessLogRecord record = new AccessLogRecord(null);
    assertEquals(record.getRecord(), "");
  }

  @Test
  public void testParse() {
    AccessLogRecord record = new AccessLogRecord(LOG_ENTRY);

    assertEquals(record.getRecord(), LOG_ENTRY);

    assertEquals(record.getIpAddr(), "62.172.72.131");
    assertEquals(record.getIdentd(), "-");
    assertEquals(record.getAuthUser(), "-");
    assertEquals(record.getTimestamp(), "[02/Jan/2003:02:06:41 -0700]");
    assertEquals(record.getRequest(), "GET /random/html/riaa_hacked/ HTTP/1.0");
    assertEquals(record.getStatus(), 200);
    assertEquals(record.getBytes(), 10564);
    assertEquals(record.getReferrer(), "-");
    assertEquals(record.getUserAgent(),
        "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 4.0; WWP 17 August 2001)");
  }

  @Test
  public void testParseRandomText() {
    AccessLogRecord record = new AccessLogRecord(RANDOM_CHARACTERS);
    assertEquals(record.getRecord(), RANDOM_CHARACTERS);
  }

  @Test
  public void testEquals() {
    AccessLogRecord record1 = new AccessLogRecord(LOG_ENTRY);
    AccessLogRecord record2 = new AccessLogRecord(LOG_ENTRY);

    assertTrue(record1.equals(record2));
    assertTrue(record2.equals(record1));
  }

  @Test
  public void testEquals2() {
    // this one doesn't use the same references for the strings.
    AccessLogRecord record1 = new AccessLogRecord("foo");
    AccessLogRecord record2 = new AccessLogRecord("f" + "oo");

    assertTrue(record1.equals(record2));
    assertTrue(record2.equals(record1));

    assertTrue(record1.equals(record1));
  }

  @Test
  public void testNotEquals() {
    AccessLogRecord record1 = new AccessLogRecord(LOG_ENTRY);
    AccessLogRecord record2 = new AccessLogRecord(RANDOM_CHARACTERS);

    assertFalse(record1.equals(record2));
    assertFalse(record2.equals(record1));
  }

}

