/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.kaiwudb.KWConnection;
import com.kaiwudb.KWProperty;
import com.kaiwudb.test.TestUtil;
import com.kaiwudb.test.util.rules.annotation.HaveMinimalServerVersion;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

@HaveMinimalServerVersion("9.4")
public class ReplicationConnectionTest {

  private Connection replConnection;

  @Before
  public void setUp() throws Exception {
    replConnection = openReplicationConnection();
    //DriverManager.setLogWriter(new PrintWriter(System.out));
  }

  @After
  public void tearDown() throws Exception {
    replConnection.close();
  }

  @Test
  public void testIsValid() throws Exception {
    boolean result = replConnection.isValid(3);

    KWConnection connection = (KWConnection) replConnection;
    connection.getBackendPID();

    assertThat("Replication connection as Simple connection can be check on valid",
        result, equalTo(true)
    );
  }

  @Test
  public void testConnectionNotValidWhenSessionTerminated() throws Exception {
    int backendId = ((KWConnection) replConnection).getBackendPID();

    Connection sqlConnection = TestUtil.openDB();

    Statement terminateStatement = sqlConnection.createStatement();
    terminateStatement.execute("SELECT pg_terminate_backend(" + backendId + ")");
    terminateStatement.close();
    sqlConnection.close();

    boolean result = replConnection.isValid(3);

    assertThat("When kaiwudb terminate session with replication connection, "
            + "isValid methos should return false, because next query on this connection will fail",
        result, equalTo(false)
    );
  }

  @Test
  public void testReplicationCommandResultSetAccessByIndex() throws Exception {
    Statement statement = replConnection.createStatement();
    ResultSet resultSet = statement.executeQuery("IDENTIFY_SYSTEM");

    String xlogpos = null;
    if (resultSet.next()) {
      xlogpos = resultSet.getString(3);
    }

    resultSet.close();
    statement.close();

    assertThat("Replication protocol supports a limited number of commands, "
            + "and it command can be execute via Statement(simple query protocol), "
            + "and result fetch via ResultSet",
        xlogpos, CoreMatchers.notNullValue()
    );
  }

  @Test
  public void testReplicationCommandResultSetAccessByName() throws Exception {
    Statement statement = replConnection.createStatement();
    ResultSet resultSet = statement.executeQuery("IDENTIFY_SYSTEM");

    String xlogpos = null;
    if (resultSet.next()) {
      xlogpos = resultSet.getString("xlogpos");
    }

    resultSet.close();
    statement.close();

    assertThat("Replication protocol supports a limited number of commands, "
            + "and it command can be execute via Statement(simple query protocol), "
            + "and result fetch via ResultSet",
        xlogpos, CoreMatchers.notNullValue()
    );
  }

  private Connection openReplicationConnection() throws Exception {
    Properties properties = new Properties();
    KWProperty.ASSUME_MIN_SERVER_VERSION.set(properties, "9.4");
    KWProperty.REPLICATION.set(properties, "database");
    //Only symple query protocol available for replication connection
    KWProperty.PREFER_QUERY_MODE.set(properties, "simple");
    return TestUtil.openDB(properties);
  }
}
