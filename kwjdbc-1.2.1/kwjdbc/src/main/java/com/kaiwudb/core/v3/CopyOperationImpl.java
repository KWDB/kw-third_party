/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core.v3;

import com.kaiwudb.util.GT;
import com.kaiwudb.copy.CopyOperation;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.sql.SQLException;

public abstract class CopyOperationImpl implements CopyOperation {
  QueryExecutorImpl queryExecutor;
  int rowFormat;
  int[] fieldFormats;
  long handledRowCount = -1;

  void init(QueryExecutorImpl q, int fmt, int[] fmts) {
    queryExecutor = q;
    rowFormat = fmt;
    fieldFormats = fmts;
  }

  @Override
  public void cancelCopy() throws SQLException {
    queryExecutor.cancelCopy(this);
  }

  @Override
  public int getFieldCount() {
    return fieldFormats.length;
  }

  @Override
  public int getFieldFormat(int field) {
    return fieldFormats[field];
  }

  @Override
  public int getFormat() {
    return rowFormat;
  }

  @Override
  public boolean isActive() {
    synchronized (queryExecutor) {
      return queryExecutor.hasLock(this);
    }
  }

  public void handleCommandStatus(String status) throws KSQLException {
    if (status.startsWith("COPY")) {
      int i = status.lastIndexOf(' ');
      handledRowCount = i > 3 ? Long.parseLong(status.substring(i + 1)) : -1;
    } else {
      throw new KSQLException(GT.tr("CommandComplete expected COPY but got: " + status),
          KSQLState.COMMUNICATION_ERROR);
    }
  }

  /**
   * Consume received copy data.
   *
   * @param data data that was receive by copy protocol
   * @throws KSQLException if some internal problem occurs
   */
  protected abstract void handleCopydata(byte[] data) throws KSQLException;

  @Override
  public long getHandledRowCount() {
    return handledRowCount;
  }
}
