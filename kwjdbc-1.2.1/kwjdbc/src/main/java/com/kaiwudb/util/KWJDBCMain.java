/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.util;

import com.kaiwudb.Driver;

public class KWJDBCMain {

  public static void main(String[] args) {

    java.net.URL url = Driver.class.getResource("/com/kaiwudb/Driver.class");
    System.out.printf("%n%s%n", DriverInfo.DRIVER_FULL_NAME);
    System.out.printf("Found in: %s%n%n", url);

    System.out.printf("The KwJDBC driver is not an executable Java program.%n%n"
                       + "You must install it according to the JDBC driver installation "
                       + "instructions for your application / container / appserver, "
                       + "then use it by specifying a JDBC URL of the form %n    jdbc:kaiwudb://%n"
                       + "or using an application specific method.%n%n");
//                       + "See the KwJDBC documentation: http://jdbc.kaiwudb.com/documentation/head/index.html%n%n"
//                       + "This command has had no effect.%n");

    System.exit(1);
  }
}
