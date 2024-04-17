/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.util;

/**
 * Utility class with constants of Driver information.
 */
public final class DriverInfo {
  private DriverInfo() {
  }

  /* Driver name */

  public static final String DRIVER_NAME = "KaiwuDB JDBC Driver";
  public static final String DRIVER_VERSION = "2.0.1";
  public static final String DRIVER_FULL_NAME = DRIVER_NAME + " " + DRIVER_VERSION;

  /* Driver version */

  public static final int MAJOR_VERSION = 2;
  public static final int MINOR_VERSION = 0;
  public static final int PATCH_VERSION = 1;

  /* JDBC specification */

  public static final String JDBC_VERSION = "2.0.1";
  private static final int JDBC_INT_VERSION = 2;
  public static final int JDBC_MAJOR_VERSION = JDBC_INT_VERSION / 10;
  public static final int JDBC_MINOR_VERSION = JDBC_INT_VERSION % 10;
}
