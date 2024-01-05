/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.xa;

import com.kaiwudb.core.BaseConnection;
import com.kaiwudb.ds.common.BaseDataSource;
import com.kaiwudb.util.DriverInfo;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Reference;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

/**
 * XA-enabled DataSource implementation.
 *
 * @author Heikki Linnakangas (heikki.linnakangas@iki.fi)
 */
public class KWXADataSource extends BaseDataSource implements XADataSource {
  /**
   * Gets a connection to the KaiwuDB database. The database is identified by the DataSource
   * properties serverName, databaseName, and portNumber. The user to connect as is identified by
   * the DataSource properties user and password.
   *
   * @return A valid database connection.
   * @throws SQLException Occurs when the database connection cannot be established.
   */
  @Override
  public XAConnection getXAConnection() throws SQLException {
    return getXAConnection(getUser(), getPassword());
  }

  /**
   * Gets a XA-enabled connection to the KaiwuDB database. The database is identified by the
   * DataSource properties serverName, databaseName, and portNumber. The user to connect as is
   * identified by the arguments user and password, which override the DataSource properties by the
   * same name.
   *
   * @return A valid database connection.
   * @throws SQLException Occurs when the database connection cannot be established.
   */
  @Override
  public XAConnection getXAConnection(String user, String password) throws SQLException {
    Connection con = super.getConnection(user, password);
    return new KWXAConnection((BaseConnection) con);
  }

  @Override
  public String getDescription() {
    return "XA-enabled DataSource from " + DriverInfo.DRIVER_FULL_NAME;
  }

  /**
   * Generates a reference using the appropriate object factory.
   */
  @Override
  protected Reference createReference() {
    return new Reference(getClass().getName(), KWXADataSourceFactory.class.getName(), null);
  }

}
