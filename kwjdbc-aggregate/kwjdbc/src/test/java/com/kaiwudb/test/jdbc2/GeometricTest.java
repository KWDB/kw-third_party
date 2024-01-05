/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.kaiwudb.core.ServerVersion;
import com.kaiwudb.geometric.KWbox;
import com.kaiwudb.geometric.KWcircle;
import com.kaiwudb.geometric.KWline;
import com.kaiwudb.geometric.KWlseg;
import com.kaiwudb.geometric.KWpath;
import com.kaiwudb.geometric.KWpoint;
import com.kaiwudb.geometric.KWpolygon;
import com.kaiwudb.test.TestUtil;
import com.kaiwudb.util.KWobject;
import com.kaiwudb.util.KSQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 * Test case for geometric type I/O
 */
@RunWith(Parameterized.class)
public class GeometricTest extends BaseTest4 {

  public GeometricTest(BinaryMode binaryMode) {
    setBinaryMode(binaryMode);
  }

  @Parameterized.Parameters(name = "binary = {0}")
  public static Iterable<Object[]> data() {
    Collection<Object[]> ids = new ArrayList<Object[]>();
    for (BinaryMode binaryMode : BinaryMode.values()) {
      ids.add(new Object[]{binaryMode});
    }
    return ids;
  }

  public void setUp() throws Exception {
    super.setUp();
    TestUtil.createTable(con, "testgeometric",
        "boxval box, circleval circle, lsegval lseg, pathval path, polygonval polygon, pointval point, lineval line");
  }

  public void tearDown() throws SQLException {
    TestUtil.dropTable(con, "testgeometric");
    super.tearDown();
  }

  private void checkReadWrite(KWobject obj, String column) throws Exception {
    PreparedStatement insert =
        con.prepareStatement("INSERT INTO testgeometric(" + column + ") VALUES (?)");
    insert.setObject(1, obj);
    insert.executeUpdate();
    insert.close();

    Statement stmt = con.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT " + column + " FROM testgeometric");
    assertTrue(rs.next());
    assertEquals("KWObject#equals(rs.getObject)", obj, rs.getObject(1));
    KWobject obj2 = (KWobject) obj.clone();
    obj2.setValue(rs.getString(1));
    assertEquals("KWobject.toString vs rs.getString", obj, obj2);
    rs.close();

    stmt.executeUpdate("DELETE FROM testgeometric");
    stmt.close();
  }

  @Test
  public void testKWbox() throws Exception {
    checkReadWrite(new KWbox(1.0, 2.0, 3.0, 4.0), "boxval");
    checkReadWrite(new KWbox(-1.0, 2.0, 3.0, 4.0), "boxval");
    checkReadWrite(new KWbox(1.0, -2.0, 3.0, 4.0), "boxval");
    checkReadWrite(new KWbox(1.0, 2.0, -3.0, 4.0), "boxval");
    checkReadWrite(new KWbox(1.0, 2.0, 3.0, -4.0), "boxval");
  }

  @Test
  public void testKWcircle() throws Exception {
    checkReadWrite(new KWcircle(1.0, 2.0, 3.0), "circleval");
    checkReadWrite(new KWcircle(-1.0, 2.0, 3.0), "circleval");
    checkReadWrite(new KWcircle(1.0, -2.0, 3.0), "circleval");
  }

  @Test
  public void testKWlseg() throws Exception {
    checkReadWrite(new KWlseg(1.0, 2.0, 3.0, 4.0), "lsegval");
    checkReadWrite(new KWlseg(-1.0, 2.0, 3.0, 4.0), "lsegval");
    checkReadWrite(new KWlseg(1.0, -2.0, 3.0, 4.0), "lsegval");
    checkReadWrite(new KWlseg(1.0, 2.0, -3.0, 4.0), "lsegval");
    checkReadWrite(new KWlseg(1.0, 2.0, 3.0, -4.0), "lsegval");
  }

  @Test
  public void testKWpath() throws Exception {
    KWpoint[] points =
        new KWpoint[]{new KWpoint(0.0, 0.0), new KWpoint(0.0, 5.0), new KWpoint(5.0, 5.0),
            new KWpoint(5.0, -5.0), new KWpoint(-5.0, -5.0), new KWpoint(-5.0, 5.0),};

    checkReadWrite(new KWpath(points, true), "pathval");
    checkReadWrite(new KWpath(points, false), "pathval");
  }

  @Test
  public void testKWpolygon() throws Exception {
    KWpoint[] points =
        new KWpoint[]{new KWpoint(0.0, 0.0), new KWpoint(0.0, 5.0), new KWpoint(5.0, 5.0),
            new KWpoint(5.0, -5.0), new KWpoint(-5.0, -5.0), new KWpoint(-5.0, 5.0),};

    checkReadWrite(new KWpolygon(points), "polygonval");
  }

  @Test
  public void testKWline() throws Exception {
    final String columnName = "lineval";

    // KaiwuDB versions older than 9.4 support creating columns with the LINE datatype, but
    // not actually writing to those columns. Only try to write if the version if at least 9.4
    final boolean roundTripToDatabase = TestUtil.haveMinimumServerVersion(con, ServerVersion.v9_4);

    if (TestUtil.haveMinimumServerVersion(con, ServerVersion.v9_4)) {

      // Apparently the driver requires public no-args constructor, and kaiwudb doesn't accept
      // lines with A and B
      // coefficients both being zero... so assert a no-arg instantiated instance throws an
      // exception.
      if (roundTripToDatabase) {
        try {
          checkReadWrite(new KWline(), columnName);
          fail("Expected a KSQLException to be thrown");
        } catch (KSQLException e) {
          assertEquals("22P02", e.getSQLState());
        }
      }

      // Generate a dataset for testing.
      List<KWline> linesToTest = new ArrayList<KWline>();
      for (double i = 1; i <= 3; i += 0.25) {
        // Test the 3-arg constructor (coefficients+constant)
        linesToTest.add(new KWline(i, (0 - i), (1 / i)));
        linesToTest.add(new KWline("{" + i + "," + (0 - i) + "," + (1 / i) + "}"));
        // Test the 4-arg constructor (x/y coords of two points on the line)
        linesToTest.add(new KWline(i, (0 - i), (1 / i), (1 / i / i)));
        linesToTest.add(new KWline(i, (0 - i), i, (1 / i / i))); // tests vertical line
        // Test 2-arg constructor (2 KWpoints on the line);
        linesToTest.add(new KWline(new KWpoint(i, (0 - i)), new KWpoint((1 / i), (1 / i / i))));
        // tests vertical line
        linesToTest.add(new KWline(new KWpoint(i, (0 - i)), new KWpoint(i, (1 / i / i))));
        // Test 1-arg constructor (KWlseg on the line);
        linesToTest.add(new KWline(new KWlseg(i, (0 - i), (1 / i), (1 / i / i))));
        linesToTest.add(new KWline(new KWlseg(i, (0 - i), i, (1 / i / i))));
        linesToTest.add(
            new KWline(new KWlseg(new KWpoint(i, (0 - i)), new KWpoint((1 / i), (1 / i / i)))));
        linesToTest
            .add(new KWline(new KWlseg(new KWpoint(i, (0 - i)), new KWpoint(i, (1 / i / i)))));
      }

      // Include persistence an querying if the kaiwudb version supports it.
      if (roundTripToDatabase) {
        for (KWline testLine : linesToTest) {
          checkReadWrite(testLine, columnName);
        }
      }

    }
  }

  @Test
  public void testKWpoint() throws Exception {
    checkReadWrite(new KWpoint(1.0, 2.0), "pointval");
  }

}
