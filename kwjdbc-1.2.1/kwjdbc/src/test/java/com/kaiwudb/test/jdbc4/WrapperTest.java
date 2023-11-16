/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.kaiwudb.KWConnection;
import com.kaiwudb.KWStatement;
import com.kaiwudb.ds.KWSimpleDataSource;
import com.kaiwudb.test.TestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class WrapperTest {

  private Connection conn;
  private Statement statement;

  @Before
  public void setUp() throws Exception {
    conn = TestUtil.openDB();
    statement = conn.prepareStatement("SELECT 1");
  }

  @After
  public void tearDown() throws SQLException {
    statement.close();
    TestUtil.closeDB(conn);
  }

  /**
   * This interface is private, and so cannot be supported by any wrapper.
   */
  private interface PrivateInterface {
  }

  @Test
  public void testConnectionIsWrapperForPrivate() throws SQLException {
    assertFalse(conn.isWrapperFor(PrivateInterface.class));
  }

  @Test
  public void testConnectionIsWrapperForConnection() throws SQLException {
    assertTrue(conn.isWrapperFor(Connection.class));
  }

  @Test
  public void testConnectionIsWrapperForKWConnection() throws SQLException {
    assertTrue(conn.isWrapperFor(KWConnection.class));
  }

  @Test
  public void testConnectionUnwrapPrivate() throws SQLException {
    try {
      conn.unwrap(PrivateInterface.class);
      fail("unwrap of non-wrapped interface should fail");
    } catch (SQLException e) {
    }
  }

  @Test
  public void testConnectionUnwrapConnection() throws SQLException {
    Object v = conn.unwrap(Connection.class);
    assertNotNull(v);
    assertTrue(v instanceof Connection);
  }

  @Test
  public void testConnectionUnwrapKWConnection() throws SQLException {
    Object v = conn.unwrap(KWConnection.class);
    assertNotNull(v);
    assertTrue(v instanceof KWConnection);
  }

  @Test
  public void testConnectionUnwrapKWDataSource() throws SQLException {
    KWSimpleDataSource dataSource = new KWSimpleDataSource();
    dataSource.setDatabaseName(TestUtil.getDatabase());
    dataSource.setServerName(TestUtil.getServer());
    dataSource.setPortNumber(TestUtil.getPort());
    Connection connection = dataSource.getConnection(TestUtil.getUser(), TestUtil.getPassword());
    assertNotNull("Unable to obtain a connection from KWSimpleDataSource", connection);
    Object v = connection.unwrap(KWConnection.class);
    assertTrue("connection.unwrap(KWConnection.class) should return KWConnection instance"
            + ", actual instance is " + v,
        v instanceof KWConnection);
  }

  @Test
  public void testStatementIsWrapperForPrivate() throws SQLException {
    assertFalse(statement.isWrapperFor(PrivateInterface.class));
  }

  @Test
  public void testStatementIsWrapperForStatement() throws SQLException {
    assertTrue(statement.isWrapperFor(Statement.class));
  }

  @Test
  public void testStatementIsWrapperForKWStatement() throws SQLException {
    assertTrue(statement.isWrapperFor(KWStatement.class));
  }

  @Test
  public void testStatementUnwrapPrivate() throws SQLException {
    try {
      statement.unwrap(PrivateInterface.class);
      fail("unwrap of non-wrapped interface should fail");
    } catch (SQLException e) {
    }
  }

  @Test
  public void testStatementUnwrapStatement() throws SQLException {
    Object v = statement.unwrap(Statement.class);
    assertNotNull(v);
    assertTrue(v instanceof Statement);
  }

  @Test
  public void testStatementUnwrapKWStatement() throws SQLException {
    Object v = statement.unwrap(KWStatement.class);
    assertNotNull(v);
    assertTrue(v instanceof KWStatement);
  }

}
