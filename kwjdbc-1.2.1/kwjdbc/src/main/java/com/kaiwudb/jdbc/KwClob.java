/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import com.kaiwudb.Driver;
import com.kaiwudb.core.BaseConnection;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Clob;
import java.sql.SQLException;

public class KwClob extends AbstractBlobClob implements java.sql.Clob {

  public KwClob(BaseConnection conn, long oid) throws java.sql.SQLException {
    super(conn, oid);
  }

  @Override
  public synchronized Reader getCharacterStream(long pos, long length) throws SQLException {
    checkFreed();
    throw Driver.notImplemented(this.getClass(), "getCharacterStream(long, long)");
  }

  @Override
  public synchronized int setString(long pos, String str) throws SQLException {
    checkFreed();
    throw Driver.notImplemented(this.getClass(), "setString(long,str)");
  }

  @Override
  public synchronized int setString(long pos, String str, int offset, int len) throws SQLException {
    checkFreed();
    throw Driver.notImplemented(this.getClass(), "setString(long,String,int,int)");
  }

  @Override
  public synchronized java.io.OutputStream setAsciiStream(long pos) throws SQLException {
    checkFreed();
    throw Driver.notImplemented(this.getClass(), "setAsciiStream(long)");
  }

  @Override
  public synchronized java.io.Writer setCharacterStream(long pos) throws SQLException {
    checkFreed();
    throw Driver.notImplemented(this.getClass(), "setCharacteStream(long)");
  }

  @Override
  public synchronized InputStream getAsciiStream() throws SQLException {
    return getBinaryStream();
  }

  @Override
  public synchronized Reader getCharacterStream() throws SQLException {
    Charset connectionCharset = Charset.forName(conn.getEncoding().name());
    return new InputStreamReader(getBinaryStream(), connectionCharset);
  }

  @Override
  public synchronized String getSubString(long i, int j) throws SQLException {
    assertPosition(i, j);
    getLo(false).seek((int) i - 1);
    return new String(getLo(false).read(j));
  }

  /**
   * For now, this is not implemented.
   */
  @Override
  public synchronized long position(String pattern, long start) throws SQLException {
    checkFreed();
    throw Driver.notImplemented(this.getClass(), "position(String,long)");
  }

  /**
   * This should be simply passing the byte value of the pattern Blob.
   */
  @Override
  public synchronized long position(Clob pattern, long start) throws SQLException {
    checkFreed();
    throw Driver.notImplemented(this.getClass(), "position(Clob,start)");
  }
}
