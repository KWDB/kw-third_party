/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.kaiwudb.KWConnection;
import com.kaiwudb.core.ServerVersion;
import com.kaiwudb.jdbc.PreferQueryMode;
import com.kaiwudb.test.TestUtil;
import com.kaiwudb.util.KWobject;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CompositeTest {

  private Connection conn;

  @BeforeClass
  public static void beforeClass() throws Exception {
    Connection conn = TestUtil.openDB();
    try {
      Assume.assumeTrue("uuid requires KaiwuDB 8.3+", TestUtil.haveMinimumServerVersion(conn, ServerVersion.v8_3));
    } finally {
      conn.close();
    }
  }

  @Before
  public void setUp() throws Exception {
    conn = TestUtil.openDB();
    TestUtil.createSchema(conn, "\"Composites\"");
    TestUtil.createCompositeType(conn, "simplecompositetest", "i int, d decimal, u uuid");
    TestUtil.createCompositeType(conn, "nestedcompositetest", "t text, s simplecompositetest");
    TestUtil.createCompositeType(conn, "\"Composites\".\"ComplexCompositeTest\"",
        "l bigint[], n nestedcompositetest[], s simplecompositetest");
    TestUtil.createTable(conn, "compositetabletest",
        "s simplecompositetest, cc \"Composites\".\"ComplexCompositeTest\"[]");
    TestUtil.createTable(conn, "\"Composites\".\"Table\"",
        "s simplecompositetest, cc \"Composites\".\"ComplexCompositeTest\"[]");
  }

  @After
  public void tearDown() throws SQLException {
    TestUtil.dropTable(conn, "\"Composites\".\"Table\"");
    TestUtil.dropTable(conn, "compositetabletest");
    TestUtil.dropType(conn, "\"Composites\".\"ComplexCompositeTest\"");
    TestUtil.dropType(conn, "nestedcompositetest");
    TestUtil.dropType(conn, "simplecompositetest");
    TestUtil.dropSchema(conn, "\"Composites\"");
    TestUtil.closeDB(conn);
  }

  @Test
  public void testSimpleSelect() throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement("SELECT '(1,2.2,)'::simplecompositetest");
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    KWobject kwo = (KWobject) rs.getObject(1);
    assertEquals("simplecompositetest", kwo.getType());
    assertEquals("(1,2.2,)", kwo.getValue());
  }

  @Test
  public void testComplexSelect() throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(
        "SELECT '(\"{1,2}\",{},\"(1,2.2,)\")'::\"Composites\".\"ComplexCompositeTest\"");
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    KWobject kwo = (KWobject) rs.getObject(1);
    assertEquals("\"Composites\".\"ComplexCompositeTest\"", kwo.getType());
    assertEquals("(\"{1,2}\",{},\"(1,2.2,)\")", kwo.getValue());
  }

  @Test
  public void testSimpleArgumentSelect() throws SQLException {
    Assume.assumeTrue("Skip if running in simple query mode", conn.unwrap(KWConnection.class).getPreferQueryMode() != PreferQueryMode.SIMPLE);
    PreparedStatement pstmt = conn.prepareStatement("SELECT ?");
    KWobject kwo = new KWobject();
    kwo.setType("simplecompositetest");
    kwo.setValue("(1,2.2,)");
    pstmt.setObject(1, kwo);
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    KWobject kwo2 = (KWobject) rs.getObject(1);
    assertEquals(kwo, kwo2);
  }

  @Test
  public void testComplexArgumentSelect() throws SQLException {
    Assume.assumeTrue("Skip if running in simple query mode", conn.unwrap(KWConnection.class).getPreferQueryMode() != PreferQueryMode.SIMPLE);
    PreparedStatement pstmt = conn.prepareStatement("SELECT ?");
    KWobject kwo = new KWobject();
    kwo.setType("\"Composites\".\"ComplexCompositeTest\"");
    kwo.setValue("(\"{1,2}\",{},\"(1,2.2,)\")");
    pstmt.setObject(1, kwo);
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    KWobject kwo2 = (KWobject) rs.getObject(1);
    assertEquals(kwo, kwo2);
  }

  @Test
  public void testCompositeFromTable() throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement("INSERT INTO compositetabletest VALUES(?, ?)");
    KWobject kwo1 = new KWobject();
    kwo1.setType("public.simplecompositetest");
    kwo1.setValue("(1,2.2,)");
    pstmt.setObject(1, kwo1);
    String[] ctArr = new String[1];
    ctArr[0] = "(\"{1,2}\",{},\"(1,2.2,)\")";
    Array kwarr1 = conn.createArrayOf("\"Composites\".\"ComplexCompositeTest\"", ctArr);
    pstmt.setArray(2, kwarr1);
    int res = pstmt.executeUpdate();
    assertEquals(1, res);
    pstmt = conn.prepareStatement("SELECT * FROM compositetabletest");
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    KWobject kwo2 = (KWobject) rs.getObject(1);
    Array kwarr2 = (Array) rs.getObject(2);
    assertEquals("simplecompositetest", kwo2.getType());
    assertEquals("\"Composites\".\"ComplexCompositeTest\"", kwarr2.getBaseTypeName());
    Object[] kwobjarr2 = (Object[]) kwarr2.getArray();
    assertEquals(1, kwobjarr2.length);
    KWobject arr2Elem = (KWobject) kwobjarr2[0];
    assertEquals("\"Composites\".\"ComplexCompositeTest\"", arr2Elem.getType());
    assertEquals("(\"{1,2}\",{},\"(1,2.2,)\")", arr2Elem.getValue());
    rs.close();
    pstmt = conn.prepareStatement("SELECT c FROM compositetabletest c");
    rs = pstmt.executeQuery();
    assertTrue(rs.next());
    KWobject kwo3 = (KWobject) rs.getObject(1);
    assertEquals("compositetabletest", kwo3.getType());
    assertEquals("(\"(1,2.2,)\",\"{\"\"(\\\\\"\"{1,2}\\\\\"\",{},\\\\\"\"(1,2.2,)\\\\\"\")\"\"}\")",
        kwo3.getValue());
  }

  @Test
  public void testNullArrayElement() throws SQLException {
    PreparedStatement pstmt =
        conn.prepareStatement("SELECT array[NULL, NULL]::compositetabletest[]");
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    Array arr = rs.getArray(1);
    assertEquals("compositetabletest", arr.getBaseTypeName());
    Object[] items = (Object[]) arr.getArray();
    assertEquals(2, items.length);
    assertNull(items[0]);
    assertNull(items[1]);
  }

  @Test
  public void testTableMetadata() throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement("INSERT INTO compositetabletest VALUES(?, ?)");
    KWobject kwo1 = new KWobject();
    kwo1.setType("public.simplecompositetest");
    kwo1.setValue("(1,2.2,)");
    pstmt.setObject(1, kwo1);
    String[] ctArr = new String[1];
    ctArr[0] = "(\"{1,2}\",{},\"(1,2.2,)\")";
    Array kwarr1 = conn.createArrayOf("\"Composites\".\"ComplexCompositeTest\"", ctArr);
    pstmt.setArray(2, kwarr1);
    int res = pstmt.executeUpdate();
    assertEquals(1, res);
    pstmt = conn.prepareStatement("SELECT t FROM compositetabletest t");
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    String name = rs.getMetaData().getColumnTypeName(1);
    assertEquals("compositetabletest", name);
  }

  @Test
  public void testComplexTableNameMetadata() throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement("INSERT INTO \"Composites\".\"Table\" VALUES(?, ?)");
    KWobject kwo1 = new KWobject();
    kwo1.setType("public.simplecompositetest");
    kwo1.setValue("(1,2.2,)");
    pstmt.setObject(1, kwo1);
    String[] ctArr = new String[1];
    ctArr[0] = "(\"{1,2}\",{},\"(1,2.2,)\")";
    Array kwarr1 = conn.createArrayOf("\"Composites\".\"ComplexCompositeTest\"", ctArr);
    pstmt.setArray(2, kwarr1);
    int res = pstmt.executeUpdate();
    assertEquals(1, res);
    pstmt = conn.prepareStatement("SELECT t FROM \"Composites\".\"Table\" t");
    ResultSet rs = pstmt.executeQuery();
    assertTrue(rs.next());
    String name = rs.getMetaData().getColumnTypeName(1);
    assertEquals("\"Composites\".\"Table\"", name);
  }
}
