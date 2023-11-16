/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.ssl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CommonNameVerifierTest.class,
    LazyKeyManagerTest.class,
    LibPQFactoryHostNameTest.class,
    SslTest.class,
})
public class SslTestSuite {
}
