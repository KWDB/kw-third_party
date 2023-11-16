/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.ssl;

import com.kaiwudb.test.jdbc2.BaseTest4;
import com.kaiwudb.KWProperty;
import com.kaiwudb.test.TestUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.ResultSet;
import java.util.Properties;

public class PKCS12KeyTest extends BaseTest4 {
  @Override
  @Before
  public void setUp() throws Exception {

    super.setUp();
  }

  @Override
  protected void updateProperties(Properties props) {
    Properties prop = TestUtil.loadPropertyFiles("ssltest.properties");
    props.put(TestUtil.DATABASE_PROP, "hostssldb");
    KWProperty.SSL_MODE.set(props, "prefer");

    File certDirFile = TestUtil.getFile(prop.getProperty("certdir"));
    String certdir = certDirFile.getAbsolutePath();

    KWProperty.SSL_KEY.set(props, certdir + "/" + "goodclient" + ".p12");

  }

  @Test
  public void TestGoodClientP12() throws Exception {

    ResultSet rs = con.createStatement().executeQuery("select ssl_is_used()");
    Assert.assertTrue("select ssl_is_used() should return a row", rs.next());
    boolean sslUsed = rs.getBoolean(1);
  }
}
