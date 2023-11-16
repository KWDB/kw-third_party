/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.extensions;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Executes all known tests for KaiwuDB extensions supported by JDBC driver
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    HStoreTest.class,
})
public class ExtensionsTestSuite {
}

