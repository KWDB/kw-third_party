/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.util;

import static org.junit.Assert.assertTrue;

import com.kaiwudb.test.TestUtil;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class KSQLWarningTest {

  @Test
  public void testPSQLLogsToDriverManagerMessage() throws Exception {
    Connection con = TestUtil.openDB();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DriverManager.setLogWriter(new PrintWriter(new OutputStreamWriter(baos, "ASCII")));

    Statement stmt = con.createStatement();
    stmt.execute("DO language plpgsql $$ BEGIN RAISE NOTICE 'test notice'; END $$;");
    assertTrue(baos.toString().contains("NOTICE: test notice"));

    stmt.close();
    con.close();
  }
}
