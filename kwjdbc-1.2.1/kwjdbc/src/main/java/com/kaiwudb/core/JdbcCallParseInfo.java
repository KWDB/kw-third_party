/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

/**
 * Contains parse flags from {@link Parser# modifyJdbcCall(String, boolean, int, int, EscapeSyntaxCallMode)}.
 */
public class JdbcCallParseInfo {
  private final String sql;
  private final boolean isFunction;

  public JdbcCallParseInfo(String sql, boolean isFunction) {
    this.sql = sql;
    this.isFunction = isFunction;
  }

  /**
   * SQL in a native for certain backend version.
   *
   * @return SQL in a native for certain backend version
   */
  public String getSql() {
    return sql;
  }

  /**
   * Returns if given SQL is a function.
   *
   * @return {@code true} if given SQL is a function
   */
  public boolean isFunction() {
    return isFunction;
  }

}
