/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc2;

import com.kaiwudb.util.ReaderInputStreamTest;
import com.kaiwudb.core.CommandCompleteParserNegativeTest;
import com.kaiwudb.core.CommandCompleteParserTest;
import com.kaiwudb.core.OidToStringTest;
import com.kaiwudb.core.OidValueOfTest;
import com.kaiwudb.core.ParserTest;
import com.kaiwudb.core.ReturningParserTest;
import com.kaiwudb.core.v3.V3ParameterListTests;
import com.kaiwudb.jdbc.DeepBatchedInsertStatementTest;
import com.kaiwudb.jdbc.KwSQLXMLTest;
import com.kaiwudb.jdbc.PrimitiveArraySupportTest;
import com.kaiwudb.test.core.JavaVersionTest;
import com.kaiwudb.test.core.LogServerMessagePropertyTest;
import com.kaiwudb.test.core.NativeQueryBindLengthTest;
import com.kaiwudb.test.core.OptionsPropertyTest;
import com.kaiwudb.test.util.ExpressionPropertiesTest;
import com.kaiwudb.test.util.HostSpecTest;
import com.kaiwudb.test.util.LruCacheTest;
import com.kaiwudb.test.util.ServerVersionParseTest;
import com.kaiwudb.test.util.ServerVersionTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Executes all known tests for JDBC2 and includes some utility methods.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ANTTest.class,
    ArrayTest.class,
    BatchedInsertReWriteEnabledTest.class,
    BatchExecuteTest.class,
    BatchFailureTest.class,
    BlobTest.class,
    BlobTransactionTest.class,
    CallableStmtTest.class,
    ClientEncodingTest.class,
    ColumnSanitiserDisabledTest.class,
    ColumnSanitiserEnabledTest.class,
    CommandCompleteParserNegativeTest.class,
    CommandCompleteParserTest.class,
    ConcurrentStatementFetch.class,
    ConnectionTest.class,
    ConnectTimeoutTest.class,
    CopyLargeFileTest.class,
    CopyTest.class,
    CursorFetchTest.class,
    DatabaseEncodingTest.class,
    DatabaseMetaDataPropertiesTest.class,
    DatabaseMetaDataTest.class,
    DateStyleTest.class,
    DateTest.class,
    DeepBatchedInsertStatementTest.class,
    DriverTest.class,
    EncodingTest.class,
    ExpressionPropertiesTest.class,
    GeometricTest.class,
    GetXXXTest.class,
    HostSpecTest.class,
    IntervalTest.class,
    JavaVersionTest.class,
    JBuilderTest.class,
    LoginTimeoutTest.class,
    LogServerMessagePropertyTest.class,
    LruCacheTest.class,
    MiscTest.class,
    NativeQueryBindLengthTest.class,
    NotifyTest.class,
    OidToStringTest.class,
    OidValueOfTest.class,
    OptionsPropertyTest.class,
    OuterJoinSyntaxTest.class,
    ParameterStatusTest.class,
    ParserTest.class,
    KWPropertyTest.class,
    KWTimestampTest.class,
    KWTimeTest.class,
    KwSQLXMLTest.class,
    PreparedStatementTest.class,
    PrimitiveArraySupportTest.class,
    QuotationTest.class,
    ReaderInputStreamTest.class,
    RefCursorTest.class,
    ReplaceProcessingTest.class,
    ResultSetMetaDataTest.class,
    ResultSetTest.class,
    ReturningParserTest.class,
    SearchPathLookupTest.class,
    ServerCursorTest.class,
    ServerErrorTest.class,
    ServerPreparedStmtTest.class,
    ServerVersionParseTest.class,
    ServerVersionTest.class,
    StatementTest.class,
    StringTypeUnspecifiedArrayTest.class,
    TestACL.class,
    TimestampTest.class,
    TimeTest.class,
    TimezoneCachingTest.class,
    TimezoneTest.class,
    TypeCacheDLLStressTest.class,
    UpdateableResultTest.class,
    UpsertTest.class,
    V3ParameterListTests.class,
})
public class Jdbc2TestSuite {
}
