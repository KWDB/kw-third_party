/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc4;

import static org.junit.Assert.assertEquals;

import com.kaiwudb.KWConnection;
import com.kaiwudb.KWResultSetMetaData;
import com.kaiwudb.KWStatement;
import com.kaiwudb.core.Field;
import com.kaiwudb.jdbc.PreferQueryMode;
import com.kaiwudb.test.jdbc2.BaseTest4;

import org.junit.Assume;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * We don't want to use the binary protocol for one-off queries as it involves another round-trip to
 * the server to 'describe' the query. If we use the query enough times (see
 * {@link KWConnection#setPrepareThreshold(int)} then we'll change to using the binary protocol to
 * save bandwidth and reduce decoding time.
 */
public class BinaryTest extends BaseTest4 {
  private ResultSet results;
  private PreparedStatement statement;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Assume.assumeTrue("Server-prepared statements are not supported in 'simple protocol only'",
        preferQueryMode != PreferQueryMode.SIMPLE);
    statement = con.prepareStatement("select 1");

    ((KWStatement) statement).setPrepareThreshold(5);
  }

  @Test
  public void testPreparedStatement_3() throws Exception {
    ((KWStatement) statement).setPrepareThreshold(3);

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    ((KWStatement) statement).setPrepareThreshold(5);
  }

  @Test
  public void testPreparedStatement_1() throws Exception {
    ((KWStatement) statement).setPrepareThreshold(1);

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    ((KWStatement) statement).setPrepareThreshold(5);
  }

  @Test
  public void testPreparedStatement_0() throws Exception {
    ((KWStatement) statement).setPrepareThreshold(0);

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.TEXT_FORMAT, getFormat(results));

    ((KWStatement) statement).setPrepareThreshold(5);
  }

  @Test
  public void testPreparedStatement_negative1() throws Exception {
    ((KWStatement) statement).setPrepareThreshold(-1);

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    results = statement.executeQuery();
    assertEquals(Field.BINARY_FORMAT, getFormat(results));

    ((KWStatement) statement).setPrepareThreshold(5);
  }

  @Test
  public void testReceiveBinary() throws Exception {
    PreparedStatement ps = con.prepareStatement("select ?");
    for (int i = 0; i < 10; i++) {
      ps.setInt(1, 42 + i);
      ResultSet rs = ps.executeQuery();
      assertEquals("One row should be returned", true, rs.next());
      assertEquals(42 + i, rs.getInt(1));
      rs.close();
    }
    ps.close();
  }

  private int getFormat(ResultSet results) throws SQLException {
    return ((KWResultSetMetaData) results.getMetaData()).getFormat(1);
  }
}
