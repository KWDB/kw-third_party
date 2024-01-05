/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.sspi;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import com.kaiwudb.test.TestUtil;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

/*
 * These tests require a working SSPI authentication setup
 * in the database server that allows the executing user
 * to authenticate as the "sspiusername" in the build
 * configuration.
 */
public class SSPITest {

  /*
   * SSPI only exists on Windows.
   */
  @BeforeClass
  public static void checkPlatform() {
    assumeThat("SSPI not supported on this platform",
               System.getProperty("os.name").toLowerCase(),
               containsString("windows"));
  }

  /*
   * Tests that SSPI login succeeds and a query can be run.
   */
  @Test
  public void testAuthorized() throws Exception {
    Properties props = new Properties();
    props.setProperty("username", TestUtil.getSSPIUser());
    Connection con = TestUtil.openDB(props);

    Statement stmt = con.createStatement();
    stmt.executeQuery("SELECT 1");

    TestUtil.closeDB(con);
  }

  /*
   * Tests that SSPI login fails with an unknown/unauthorized
   * user name.
   */
  @Test
  public void testUnauthorized() throws Exception {
    Properties props = new Properties();
    props.setProperty("username", "invalid" + TestUtil.getSSPIUser());

    try {
      Connection con = TestUtil.openDB(props);
      TestUtil.closeDB(con);
      fail("Expected a KSQLException");
    } catch (KSQLException e) {
      assertThat(e.getSQLState(), is(KSQLState.INVALID_AUTHORIZATION_SPECIFICATION.getState()));
    }
  }

}
