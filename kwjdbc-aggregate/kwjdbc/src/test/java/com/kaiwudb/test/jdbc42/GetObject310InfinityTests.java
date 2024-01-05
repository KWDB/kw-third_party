/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc42;

import com.kaiwudb.core.ServerVersion;
import com.kaiwudb.test.TestUtil;
import com.kaiwudb.test.jdbc2.BaseTest4;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class GetObject310InfinityTests extends BaseTest4 {
  private final String expression;
  private final String kwType;
  private final Class<?> klass;
  private final Object expectedValue;

  public GetObject310InfinityTests(BinaryMode binaryMode, String expression,
      String kwType, Class<?> klass, Object expectedValue) {
    setBinaryMode(binaryMode);
    this.expression = expression;
    this.kwType = kwType;
    this.klass = klass;
    this.expectedValue = expectedValue;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Assume.assumeTrue("KaiwuDB 8.3 does not support 'infinity' for 'date'",
        !"date".equals(kwType) || TestUtil.haveMinimumServerVersion(con, ServerVersion.v8_4));
  }

  @Parameterized.Parameters(name = "binary = {0}, expr = {1}, kwType = {2}, klass = {3}")
  public static Iterable<Object[]> data() throws IllegalAccessException {
    Collection<Object[]> ids = new ArrayList<Object[]>();
    for (BinaryMode binaryMode : BinaryMode.values()) {
      for (String expression : Arrays.asList("-infinity", "infinity")) {
        for (String kwType : Arrays.asList("date", "timestamp",
            "timestamp with time zone")) {
          for (Class<?> klass : Arrays.asList(LocalDate.class, LocalDateTime.class,
              OffsetDateTime.class)) {
            if (klass.equals(LocalDate.class) && !kwType.equals("date")) {
              continue;
            }
            if (klass.equals(LocalDateTime.class) && !kwType.startsWith("timestamp")) {
              continue;
            }
            if (klass.equals(OffsetDateTime.class) && !kwType.startsWith("timestamp")) {
              continue;
            }
            if (klass.equals(LocalDateTime.class) && kwType.equals("timestamp with time zone")) {
              // KSQLException: Cannot convert the column of type TIMESTAMPTZ to requested type timestamp.
              continue;
            }
            Field field = null;
            try {
              field = klass.getField(expression.startsWith("-") ? "MIN" : "MAX");
            } catch (NoSuchFieldException e) {
              throw new IllegalStateException("No min/max field in " + klass, e);
            }
            Object expected = field.get(null);
            ids.add(new Object[]{binaryMode, expression, kwType, klass, expected});
          }
        }
      }
    }
    return ids;
  }

  @Test
  public void test() throws SQLException {
    PreparedStatement stmt = con.prepareStatement("select '" + expression + "'::" + kwType);
    ResultSet rs = stmt.executeQuery();
    rs.next();
    Object res = rs.getObject(1, klass);
    Assert.assertEquals(expectedValue, res);
  }
}
