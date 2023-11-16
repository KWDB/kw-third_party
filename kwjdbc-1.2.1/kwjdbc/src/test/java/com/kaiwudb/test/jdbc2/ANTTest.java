/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ANTTest {

  /*
   * This tests the acceptsURL() method with a couple of good and badly formed jdbc urls
   */
  @Test
  public void testANT() {
    String url = System.getProperty("database");
    String usr = System.getProperty("username");
    String psw = System.getProperty("password");

    assertNotNull(url);
    assertNotNull(usr);
    assertNotNull(psw);

    assertFalse(url.isEmpty());
    assertFalse(usr.isEmpty());
  }
}
