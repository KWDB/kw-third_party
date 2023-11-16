/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.kaiwudb.test.TestUtil;
import com.kaiwudb.util.KWInterval;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class IntervalTest {
  private Connection conn;

  @Before
  public void setUp() throws Exception {
    conn = TestUtil.openDB();
    TestUtil.createTable(conn, "testinterval", "v interval");
    TestUtil.createTable(conn, "testdate", "v date");
  }

  @After
  public void tearDown() throws Exception {
    TestUtil.dropTable(conn, "testinterval");
    TestUtil.dropTable(conn, "testdate");

    TestUtil.closeDB(conn);
  }

  @Test
  public void testOnlineTests() throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement("INSERT INTO testinterval VALUES (?)");
    pstmt.setObject(1, new KWInterval(2004, 13, 28, 0, 0, 43000.9013));
    pstmt.executeUpdate();
    pstmt.close();

    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT v FROM testinterval");
    assertTrue(rs.next());
    KWInterval kwi = (KWInterval) rs.getObject(1);
    assertEquals(2005, kwi.getYears());
    assertEquals(1, kwi.getMonths());
    assertEquals(28, kwi.getDays());
    assertEquals(11, kwi.getHours());
    assertEquals(56, kwi.getMinutes());
    assertEquals(40.9013, kwi.getSeconds(), 0.000001);
    assertTrue(!rs.next());
    rs.close();
    stmt.close();
  }

  @Test
  public void testStringToIntervalCoercion() throws SQLException {
    Statement stmt = conn.createStatement();
    stmt.executeUpdate(TestUtil.insertSQL("testdate", "'2010-01-01'"));
    stmt.executeUpdate(TestUtil.insertSQL("testdate", "'2010-01-02'"));
    stmt.executeUpdate(TestUtil.insertSQL("testdate", "'2010-01-04'"));
    stmt.executeUpdate(TestUtil.insertSQL("testdate", "'2010-01-05'"));
    stmt.close();

    PreparedStatement pstmt = conn.prepareStatement(
        "SELECT v FROM testdate WHERE v < (?::timestamp with time zone + ? * ?::interval) ORDER BY v");
    pstmt.setObject(1, makeDate(2010, 1, 1));
    pstmt.setObject(2, Integer.valueOf(2));
    pstmt.setObject(3, "1 day");
    ResultSet rs = pstmt.executeQuery();

    assertNotNull(rs);

    java.sql.Date d;

    assertTrue(rs.next());
    d = rs.getDate(1);
    assertNotNull(d);
    assertEquals(makeDate(2010, 1, 1), d);

    assertTrue(rs.next());
    d = rs.getDate(1);
    assertNotNull(d);
    assertEquals(makeDate(2010, 1, 2), d);

    assertFalse(rs.next());

    rs.close();
    pstmt.close();
  }

  @Test
  public void testIntervalToStringCoercion() throws SQLException {
    KWInterval interval = new KWInterval("1 year 3 months");
    String coercedStringValue = interval.toString();

    assertEquals("1 years 3 mons 0 days 0 hours 0 mins 0.0 secs", coercedStringValue);
  }

  @Test
  public void testDaysHours() throws SQLException {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT '101:12:00'::interval");
    assertTrue(rs.next());
    KWInterval i = (KWInterval) rs.getObject(1);
    // 8.1 servers store hours and days separately.
    assertEquals(0, i.getDays());
    assertEquals(101, i.getHours());

    assertEquals(12, i.getMinutes());
  }

  @Test
  public void testAddRounding() {
    KWInterval kwi = new KWInterval(0, 0, 0, 0, 0, 0.6006);
    Calendar cal = Calendar.getInstance();
    long origTime = cal.getTime().getTime();
    kwi.add(cal);
    long newTime = cal.getTime().getTime();
    assertEquals(601, newTime - origTime);
    kwi.setSeconds(-0.6006);
    kwi.add(cal);
    assertEquals(origTime, cal.getTime().getTime());
  }

  @Test
  public void testOfflineTests() throws Exception {
    KWInterval kwi = new KWInterval(2004, 4, 20, 15, 57, 12.1);

    assertEquals(2004, kwi.getYears());
    assertEquals(4, kwi.getMonths());
    assertEquals(20, kwi.getDays());
    assertEquals(15, kwi.getHours());
    assertEquals(57, kwi.getMinutes());
    assertEquals(12.1, kwi.getSeconds(), 0);

    KWInterval kwi2 = new KWInterval("@ 2004 years 4 mons 20 days 15 hours 57 mins 12.1 secs");
    assertEquals(kwi, kwi2);

    // Singular units
    KWInterval kwi3 = new KWInterval("@ 2004 year 4 mon 20 day 15 hour 57 min 12.1 sec");
    assertEquals(kwi, kwi3);

    KWInterval kwi4 = new KWInterval("2004 years 4 mons 20 days 15:57:12.1");
    assertEquals(kwi, kwi4);

    // Ago test
    kwi = new KWInterval("@ 2004 years 4 mons 20 days 15 hours 57 mins 12.1 secs ago");
    assertEquals(-2004, kwi.getYears());
    assertEquals(-4, kwi.getMonths());
    assertEquals(-20, kwi.getDays());
    assertEquals(-15, kwi.getHours());
    assertEquals(-57, kwi.getMinutes());
    assertEquals(-12.1, kwi.getSeconds(), 0);

    // Char test
    kwi = new KWInterval("@ +2004 years -4 mons +20 days -15 hours +57 mins -12.1 secs");
    assertEquals(2004, kwi.getYears());
    assertEquals(-4, kwi.getMonths());
    assertEquals(20, kwi.getDays());
    assertEquals(-15, kwi.getHours());
    assertEquals(57, kwi.getMinutes());
    assertEquals(-12.1, kwi.getSeconds(), 0);
  }

  private Calendar getStartCalendar() {
    Calendar cal = new GregorianCalendar();
    cal.set(Calendar.YEAR, 2005);
    cal.set(Calendar.MONTH, 4);
    cal.set(Calendar.DAY_OF_MONTH, 29);
    cal.set(Calendar.HOUR_OF_DAY, 15);
    cal.set(Calendar.MINUTE, 35);
    cal.set(Calendar.SECOND, 42);
    cal.set(Calendar.MILLISECOND, 100);

    return cal;
  }

  @Test
  public void testCalendar() throws Exception {
    Calendar cal = getStartCalendar();

    KWInterval kwi = new KWInterval("@ 1 year 1 mon 1 day 1 hour 1 minute 1 secs");
    kwi.add(cal);

    assertEquals(2006, cal.get(Calendar.YEAR));
    assertEquals(5, cal.get(Calendar.MONTH));
    assertEquals(30, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(16, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(36, cal.get(Calendar.MINUTE));
    assertEquals(43, cal.get(Calendar.SECOND));
    assertEquals(100, cal.get(Calendar.MILLISECOND));

    kwi = new KWInterval("@ 1 year 1 mon 1 day 1 hour 1 minute 1 secs ago");
    kwi.add(cal);

    assertEquals(2005, cal.get(Calendar.YEAR));
    assertEquals(4, cal.get(Calendar.MONTH));
    assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(35, cal.get(Calendar.MINUTE));
    assertEquals(42, cal.get(Calendar.SECOND));
    assertEquals(100, cal.get(Calendar.MILLISECOND));

    cal = getStartCalendar();

    kwi = new KWInterval("@ 1 year -23 hours -3 mins -3.30 secs");
    kwi.add(cal);

    assertEquals(2006, cal.get(Calendar.YEAR));
    assertEquals(4, cal.get(Calendar.MONTH));
    assertEquals(28, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(16, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(32, cal.get(Calendar.MINUTE));
    assertEquals(38, cal.get(Calendar.SECOND));
    assertEquals(800, cal.get(Calendar.MILLISECOND));

    kwi = new KWInterval("@ 1 year -23 hours -3 mins -3.30 secs ago");
    kwi.add(cal);

    assertEquals(2005, cal.get(Calendar.YEAR));
    assertEquals(4, cal.get(Calendar.MONTH));
    assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(35, cal.get(Calendar.MINUTE));
    assertEquals(42, cal.get(Calendar.SECOND));
    assertEquals(100, cal.get(Calendar.MILLISECOND));
  }

  @Test
  public void testDate() throws Exception {
    Date date = getStartCalendar().getTime();
    Date date2 = getStartCalendar().getTime();

    KWInterval kwi = new KWInterval("@ +2004 years -4 mons +20 days -15 hours +57 mins -12.1 secs");
    kwi.add(date);

    KWInterval kwi2 =
        new KWInterval("@ +2004 years -4 mons +20 days -15 hours +57 mins -12.1 secs ago");
    kwi2.add(date);

    assertEquals(date2, date);
  }

  @Test
  public void testPostgresDate() throws Exception {
    Date date = getStartCalendar().getTime();
    Date date2 = getStartCalendar().getTime();

    KWInterval kwi = new KWInterval("+2004 years -4 mons +20 days -15:57:12.1");
    kwi.add(date);

    KWInterval kwi2 = new KWInterval("-2004 years 4 mons -20 days 15:57:12.1");
    kwi2.add(date);

    assertEquals(date2, date);
  }

  @Test
  public void testISO8601() throws Exception {
    KWInterval kwi = new KWInterval("P1Y2M3DT4H5M6S");
    assertEquals(1, kwi.getYears() );
    assertEquals(2, kwi.getMonths() );
    assertEquals(3, kwi.getDays() );
    assertEquals(4, kwi.getHours() );
    assertEquals( 5, kwi.getMinutes() );
    assertEquals( 6, kwi.getSeconds(), .1 );

    kwi = new KWInterval("P-1Y2M3DT4H5M6S");
    assertEquals(-1, kwi.getYears());

    kwi = new KWInterval("P1Y2M");
    assertEquals(1,kwi.getYears());
    assertEquals(2, kwi.getMonths());
    assertEquals(0, kwi.getDays());

    kwi = new KWInterval("P3DT4H5M6S");
    assertEquals(0,kwi.getYears());

    kwi = new KWInterval("P-1Y-2M3DT-4H-5M-6S");
    assertEquals(-1, kwi.getYears());
    assertEquals(-2, kwi.getMonths());
    assertEquals(-4, kwi.getHours());
  }

  private java.sql.Date makeDate(int y, int m, int d) {
    return new java.sql.Date(y - 1900, m - 1, d);
  }

}
