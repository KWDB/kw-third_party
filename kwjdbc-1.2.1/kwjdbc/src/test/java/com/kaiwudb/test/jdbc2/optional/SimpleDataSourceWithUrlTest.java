/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc2.optional;

import com.kaiwudb.jdbc2.optional.SimpleDataSource;
import com.kaiwudb.test.TestUtil;

/**
 * Performs the basic tests defined in the superclass. Just adds the configuration logic.
 *
 * @author Aaron Mulder (ammulder@chariotsolutions.com)
 */
public class SimpleDataSourceWithUrlTest extends BaseDataSourceTest {
  /**
   * Creates and configures a new SimpleDataSource.
   */
  @Override
  protected void initializeDataSource() {
    if (bds == null) {
      bds = new SimpleDataSource();
      bds.setUrl("jdbc:kaiwudb://" + TestUtil.getServer() + ":" + TestUtil.getPort() + "/"
          + TestUtil.getDatabase() + "?prepareThreshold=" + TestUtil.getPrepareThreshold()
          + "&logLevel=" + TestUtil.getLogLevel());
      bds.setUser(TestUtil.getUser());
      bds.setPassword(TestUtil.getPassword());
      bds.setProtocolVersion(TestUtil.getProtocolVersion());
    }
  }
}
