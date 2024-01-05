/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.socketfactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.kaiwudb.KWProperty;
import com.kaiwudb.test.TestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.util.Properties;

public class SocketFactoryTestSuite {

  private static final String STRING_ARGUMENT = "name of a socket";

  private Connection conn;

  @Before
  public void setUp() throws Exception {
    Properties properties = new Properties();
    properties.put(KWProperty.SOCKET_FACTORY.getName(), CustomSocketFactory.class.getName());
    properties.put(KWProperty.SOCKET_FACTORY_ARG.getName(), STRING_ARGUMENT);
    conn = TestUtil.openDB(properties);
  }

  @After
  public void tearDown() throws Exception {
    TestUtil.closeDB(conn);
  }

  /**
   * Test custom socket factory.
   */
  @Test
  public void testDatabaseMetaData() throws Exception {
    assertNotNull("Custom socket factory not null", CustomSocketFactory.getInstance());
    assertEquals(STRING_ARGUMENT, CustomSocketFactory.getInstance().getArgument());
    assertEquals(1, CustomSocketFactory.getInstance().getSocketCreated());
  }

}
