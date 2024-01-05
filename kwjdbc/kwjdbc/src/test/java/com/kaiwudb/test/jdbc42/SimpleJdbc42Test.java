/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc42;

import static org.junit.Assert.assertTrue;

import com.kaiwudb.test.jdbc2.BaseTest4;

import org.junit.Test;

/**
 * Most basic test to check that the right package is compiled.
 */
public class SimpleJdbc42Test extends BaseTest4 {

  /**
   * Test presence of JDBC 4.2 specific methods.
   */
  @Test
  public void testSupportsRefCursors() throws Exception {
    assertTrue(con.getMetaData().supportsRefCursors());
  }
}
