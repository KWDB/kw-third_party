/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

import com.kaiwudb.util.KSQLException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class OidToStringTest {
  @Parameterized.Parameter(0)
  public int value;
  @Parameterized.Parameter(1)
  public String expected;

  @Parameterized.Parameters(name = "expected={1}, value={0}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {142, "XML"},
        {0, "UNSPECIFIED"},
        {-235, "<unknown:-235>"},
    });
  }

  @Test
  public void run() throws KSQLException {
    Assert.assertEquals(expected, Oid.toString(value));
  }
}
