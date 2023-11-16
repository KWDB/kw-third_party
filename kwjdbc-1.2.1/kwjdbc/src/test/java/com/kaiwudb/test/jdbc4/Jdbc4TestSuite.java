/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc4;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Executes all known tests for JDBC4
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ArrayTest.class,
    BinaryStreamTest.class,
    BinaryTest.class,
    BlobTest.class,
    CharacterStreamTest.class,
    ClientInfoTest.class,
    DatabaseMetaDataHideUnprivilegedObjectsTest.class,
    DatabaseMetaDataTest.class,
    IsValidTest.class,
    JsonbTest.class,
    LogTest.class,
    KWCopyInputStreamTest.class,
    UUIDTest.class,
    WrapperTest.class,
    XmlTest.class,
})
public class Jdbc4TestSuite {
}

