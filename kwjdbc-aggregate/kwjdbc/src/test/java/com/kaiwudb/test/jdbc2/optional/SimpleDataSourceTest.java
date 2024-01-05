/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc2.optional;

import com.kaiwudb.ds.KWSimpleDataSource;
import com.kaiwudb.jdbc2.optional.SimpleDataSource;

import org.junit.Test;

/**
 * Performs the basic tests defined in the superclass. Just adds the configuration logic.
 *
 * @author Aaron Mulder (ammulder@chariotsolutions.com)
 */
public class SimpleDataSourceTest extends BaseDataSourceTest {

  /**
   * Creates and configures a new SimpleDataSource.
   */
  @Override
  protected void initializeDataSource() {
    if (bds == null) {
      bds = new SimpleDataSource();
      setupDataSource(bds);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTypoPostgresUrl() {
    KWSimpleDataSource ds = new KWSimpleDataSource();
    // this should fail because the protocol is wrong.
    ds.setUrl("jdbc:kaiwudb://localhost:26257/test");
  }
}
