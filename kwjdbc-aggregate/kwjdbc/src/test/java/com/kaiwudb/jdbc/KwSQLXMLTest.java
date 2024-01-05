/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.kaiwudb.test.jdbc2.BaseTest4;
import com.kaiwudb.test.TestUtil;

import org.junit.Before;
import org.junit.Test;

import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLXML;
import java.sql.Statement;

public class KwSQLXMLTest extends BaseTest4 {

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    TestUtil.createTempTable(con, "xmltab","x xml");
  }

  @Test
  public void setCharacterStream() throws  Exception {
    String exmplar = "<x>value</x>";
    SQLXML kwSQLXML = con.createSQLXML();
    Writer writer = kwSQLXML.setCharacterStream();
    writer.write(exmplar);
    PreparedStatement preparedStatement = con.prepareStatement("insert into xmltab values (?)");
    preparedStatement.setSQLXML(1,kwSQLXML);
    preparedStatement.execute();

    Statement statement = con.createStatement();
    ResultSet rs = statement.executeQuery("select * from xmltab");
    assertTrue(rs.next());
    SQLXML result = rs.getSQLXML(1);
    assertNotNull(result);
    assertEquals(exmplar, result.getString());
  }
}
