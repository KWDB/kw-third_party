/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb;

import com.kaiwudb.core.Field;

import java.sql.SQLException;

public interface KWResultSetMetaData {

  /**
   * Returns the underlying column name of a query result, or "" if it is unable to be determined.
   *
   * @param column column position (1-based)
   * @return underlying column name of a query result
   * @throws SQLException if something wrong happens
   * @since 8.0
   */
  String getBaseColumnName(int column) throws SQLException;

  /**
   * Returns the underlying table name of query result, or "" if it is unable to be determined.
   *
   * @param column column position (1-based)
   * @return underlying table name of query result
   * @throws SQLException if something wrong happens
   * @since 8.0
   */
  String getBaseTableName(int column) throws SQLException;

  /**
   * Returns the underlying schema name of query result, or "" if it is unable to be determined.
   *
   * @param column column position (1-based)
   * @return underlying schema name of query result
   * @throws SQLException if something wrong happens
   * @since 8.0
   */
  String getBaseSchemaName(int column) throws SQLException;

  /**
   * Is a column Text or Binary?
   *
   * @param column column position (1-based)
   * @return 0 if column data foramt is TEXT, or 1 if BINARY
   * @throws SQLException if something wrong happens
   * @see Field#BINARY_FORMAT
   * @see Field#TEXT_FORMAT
   * @since 9.4
   */
  int getFormat(int column) throws SQLException;
}
