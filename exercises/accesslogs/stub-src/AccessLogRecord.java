// (c) Copyright 2009 Cloudera, Inc.

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AccessLogRecord
 *
 * Parses log files in the Extended Common Log Format.
 * space-delimited; "-" means a field is missing
 *
 * Common log Fields:
 *
 *   IP_addr                             192.168.2.1
 *   identd                              - (nearly always "-")
 *   http_auth_username                  frank (nearly always "-")
 *   [DD/MMM/YYYY:hh:mm:ss TZ]           [10/Aug/2008:12:41:04 -0800]
 *   "request string"                    "GET /foo.html HTTP/1.1"
 *   return_code                         200, 404
 *   return size in bytes                32440 (excludes headers)
 *
 * Extended fields:
 *
 *   "referrer url"                      "http://www.google.com/"
 *   "user-agent"                        "Mozilla/4.0 (compatible; MSIE 6.0)"
 */
public class AccessLogRecord {

  // the actual line from the access_log file
  private String record;

  // the fields on the line.
  private String ipAddr;
  private String identd;
  private String authUser;
  private String timestamp;
  private String request;
  private int status;
  private int bytes;
  private String referrer;
  private String userAgent;


  public AccessLogRecord(final String record) {
    if (record == null) {
      this.record = "";
    } else {
      this.record = record;
    }

    this.parse();
  }

  // Match fields that are space-separated, or enclosed in "double quotes,"
  // or enclosed in [square brackets].
  private static final String FIELD_REGEX = "(\"[^\"]*\")|(\\[[^\\]]*\\])|([^ ]+)";
  private static final Pattern FIELD_PATTERN = Pattern.compile(FIELD_REGEX);

  /**
   * Turns strings of the form "\"foo\"" into "foo"
   */
  static String stripQuotes(String input) {
    if (input.startsWith("\"")) {
      input = input.substring(1);
    }

    if (input.endsWith("\"")) {
      input = input.substring(0, input.length() - 1);
    }

    return input;
  }

  /** Actually parse the line into fields */
  protected void parse() {

    Matcher m = FIELD_PATTERN.matcher(this.record);

    if (m.find()) {
      this.ipAddr = this.record.substring(m.start(), m.end());
    }

    if (m.find()) {
      this.identd = this.record.substring(m.start(), m.end());
    }

    if (m.find()) {
      this.authUser = this.record.substring(m.start(), m.end());
    }

    if (m.find()) {
      this.timestamp = this.record.substring(m.start(), m.end());
    }

    if (m.find()) {
      this.request = stripQuotes(this.record.substring(m.start(), m.end()));
    }

    try {
      if (m.find()) {
        this.status = Integer.parseInt(this.record.substring(m.start(), m.end()));
      }

      if (m.find()) {
        this.bytes = Integer.parseInt(this.record.substring(m.start(), m.end()));
      }
    } catch (NumberFormatException nfe) {
      // line seems malformed; stop parsing.
      return;
    }

    if (m.find()) {
      this.referrer = stripQuotes(this.record.substring(m.start(), m.end()));
    }

    if (m.find()) {
      this.userAgent = stripQuotes(this.record.substring(m.start(), m.end()));
    }
  }

  // getters

  /** Return the record */
  public String getRecord() {
    return this.record;
  }

  public String getIpAddr() {
    return this.ipAddr;
  }

  public String getIdentd() {
    return this.identd;
  }

  public String getAuthUser() {
    return this.authUser;
  }
  public String getTimestamp() {
    return this.timestamp;
  }

  public String getRequest() {
    return this.request;
  }

  public int getStatus() {
    return this.status;
  }

  public int getBytes() {
    return this.bytes;
  }
  public String getReferrer() {
    return this.referrer;
  }

  public String getUserAgent() {
    return this.userAgent;
  }

  // methods for general compatibility...
  public String toString() {
    return this.getRecord();
  }

  public boolean equals(Object other) {
    return other instanceof AccessLogRecord && other != null
        && this.record.equals(((AccessLogRecord)other).getRecord());
  }

  public int hashCode() {
    return this.record.hashCode();
  }
}
