/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import com.kaiwudb.core.Utils;
import com.kaiwudb.util.GT;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.sql.SQLException;
import java.sql.Savepoint;

public class KSQLSavepoint implements Savepoint {

  private boolean isValid;
  private final boolean isNamed;
  private int id;
  private String name;

  public KSQLSavepoint(int id) {
    this.isValid = true;
    this.isNamed = false;
    this.id = id;
  }

  public KSQLSavepoint(String name) {
    this.isValid = true;
    this.isNamed = true;
    this.name = name;
  }

  @Override
  public int getSavepointId() throws SQLException {
    if (!isValid) {
      throw new KSQLException(GT.tr("Cannot reference a savepoint after it has been released."),
        KSQLState.INVALID_SAVEPOINT_SPECIFICATION);
    }

    if (isNamed) {
      throw new KSQLException(GT.tr("Cannot retrieve the id of a named savepoint."),
        KSQLState.WRONG_OBJECT_TYPE);
    }

    return id;
  }

  @Override
  public String getSavepointName() throws SQLException {
    if (!isValid) {
      throw new KSQLException(GT.tr("Cannot reference a savepoint after it has been released."),
        KSQLState.INVALID_SAVEPOINT_SPECIFICATION);
    }

    if (!isNamed) {
      throw new KSQLException(GT.tr("Cannot retrieve the name of an unnamed savepoint."),
        KSQLState.WRONG_OBJECT_TYPE);
    }

    return name;
  }

  public void invalidate() {
    isValid = false;
  }

  public String getKWName() throws SQLException {
    if (!isValid) {
      throw new KSQLException(GT.tr("Cannot reference a savepoint after it has been released."),
        KSQLState.INVALID_SAVEPOINT_SPECIFICATION);
    }

    if (isNamed) {
      // We need to quote and escape the name in case it
      // contains spaces/quotes/etc.
      //
      return Utils.escapeIdentifier(null, name).toString();
    }

    return "JDBC_SAVEPOINT_" + id;
  }
}
