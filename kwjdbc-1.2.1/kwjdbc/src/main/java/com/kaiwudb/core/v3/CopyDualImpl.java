/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core.v3;

import com.kaiwudb.copy.CopyDual;
import com.kaiwudb.util.KSQLException;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

public class CopyDualImpl extends CopyOperationImpl implements CopyDual {
  private Queue<byte[]> received = new LinkedList<byte[]>();

  @Override
  public void writeToCopy(byte[] data, int off, int siz) throws SQLException {
    queryExecutor.writeToCopy(this, data, off, siz);
  }

  @Override
  public void flushCopy() throws SQLException {
    queryExecutor.flushCopy(this);
  }

  @Override
  public long endCopy() throws SQLException {
    return queryExecutor.endCopy(this);
  }

  @Override
  public byte[] readFromCopy() throws SQLException {
    return readFromCopy(true);
  }

  @Override
  public byte[] readFromCopy(boolean block) throws SQLException {
    if (received.isEmpty()) {
      queryExecutor.readFromCopy(this, block);
    }

    return received.poll();
  }

  @Override
  public void handleCommandStatus(String status) throws KSQLException {
  }

  @Override
  protected void handleCopydata(byte[] data) {
    received.add(data);
  }
}
