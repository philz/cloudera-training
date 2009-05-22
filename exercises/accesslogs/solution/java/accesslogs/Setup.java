// (c) Copyright 2009 Cloudera, Inc.

package accesslogs;

import org.apache.hadoop.util.ToolRunner;

/**
 * Performs all the MapReduce jobs necessary to set up the data load-ins from mysql.
 * Outputs on stdout the list of Hive commands to run next.
 *
 * Suggested usage:
 * $HADOOP_HOME/bin/hadoop jar (thisjar) accesslogs.Setup > hive-script
 * $HIVE_HOME/bin/hive -f hive-script
 */
public final class Setup {

  // private c'tor to disable instantiation.
  private Setup() { }

  /** print the Hive commands to create the database */
  private static void emitSchemaScript() {
    System.out.println("CREATE TABLE ip_locations"
        + " (a STRING, b STRING, c STRING, cityid STRING, countryid STRING);");
    System.out.println("CREATE TABLE city_names (cityid STRING, cityname STRING);");
    System.out.println("CREATE TABLE country_names (countryid STRING, countryname STRING);");
  }

  public static void main(String [] args) throws Exception {
    emitSchemaScript();

    int ret = ToolRunner.run(new TableImports(), args);
    if (ret != 0) {
      System.exit(ret);
    }

    ret = ToolRunner.run(new IpAddrImport(), args);
    if (ret != 0) {
      System.exit(ret);
    }
  }
}

