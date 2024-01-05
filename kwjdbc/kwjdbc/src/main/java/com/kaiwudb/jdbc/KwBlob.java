/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import com.kaiwudb.core.BaseConnection;
import com.kaiwudb.largeobject.LargeObject;

import java.sql.SQLException;

public class KwBlob extends AbstractBlobClob implements java.sql.Blob {

  public KwBlob(BaseConnection conn, long oid) throws SQLException {
    super(conn, oid);
  }

  @Override
  public synchronized java.io.InputStream getBinaryStream(long pos, long length)
      throws SQLException {
    checkFreed();
    LargeObject subLO = getLo(false).copy();
    addSubLO(subLO);
    if (pos > Integer.MAX_VALUE) {
      subLO.seek64(pos - 1, LargeObject.SEEK_SET);
    } else {
      subLO.seek((int) pos - 1, LargeObject.SEEK_SET);
    }
    return subLO.getInputStream(length);
  }

  @Override
  public synchronized int setBytes(long pos, byte[] bytes) throws SQLException {
    return setBytes(pos, bytes, 0, bytes.length);
  }

  @Override
  public synchronized int setBytes(long pos, byte[] bytes, int offset, int len)
      throws SQLException {
    assertPosition(pos);
    getLo(true).seek((int) (pos - 1));
    getLo(true).write(bytes, offset, len);
    return len;
  }
}
