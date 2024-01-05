/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc3;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Executes all known tests for JDBC3
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    CompositeQueryParseTest.class,
    CompositeTest.class,
    DatabaseMetaDataTest.class,
    EscapeSyntaxCallModeCallTest.class,
    EscapeSyntaxCallModeCallIfNoReturnTest.class,
    EscapeSyntaxCallModeSelectTest.class,
    GeneratedKeysTest.class,
    Jdbc3BlobTest.class,
    Jdbc3CallableStatementTest.class,
    Jdbc3SavepointTest.class,
    ParameterMetaDataTest.class,
    ResultSetTest.class,
    SendRecvBufferSizeTest.class,
    SqlCommandParseTest.class,
    StringTypeParameterTest.class,
    TypesTest.class,
})
public class Jdbc3TestSuite {

}
