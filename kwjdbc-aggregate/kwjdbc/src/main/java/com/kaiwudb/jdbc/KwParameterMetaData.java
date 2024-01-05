/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import com.kaiwudb.util.GT;
import com.kaiwudb.util.KSQLState;
import com.kaiwudb.core.BaseConnection;
import com.kaiwudb.util.KSQLException;

import java.sql.ParameterMetaData;
import java.sql.SQLException;

public class KwParameterMetaData implements ParameterMetaData {

  private final BaseConnection connection;
  private final int[] oids;

  public KwParameterMetaData(BaseConnection connection, int[] oids) {
    this.connection = connection;
    this.oids = oids;
  }

  @Override
  public String getParameterClassName(int param) throws SQLException {
    checkParamIndex(param);
    return connection.getTypeInfo().getJavaClass(oids[param - 1]);
  }

  @Override
  public int getParameterCount() {
    return oids.length;
  }

  /**
   * {@inheritDoc} For now report all parameters as inputs. CallableStatements may have one output,
   * but ignore that for now.
   */
  @Override
  public int getParameterMode(int param) throws SQLException {
    checkParamIndex(param);
    return ParameterMetaData.parameterModeIn;
  }

  @Override
  public int getParameterType(int param) throws SQLException {
    checkParamIndex(param);
    return connection.getTypeInfo().getSQLType(oids[param - 1]);
  }

  @Override
  public String getParameterTypeName(int param) throws SQLException {
    checkParamIndex(param);
    return connection.getTypeInfo().getKWType(oids[param - 1]);
  }

  // we don't know this
  @Override
  public int getPrecision(int param) throws SQLException {
    checkParamIndex(param);
    return 0;
  }

  // we don't know this
  @Override
  public int getScale(int param) throws SQLException {
    checkParamIndex(param);
    return 0;
  }

  // we can't tell anything about nullability
  @Override
  public int isNullable(int param) throws SQLException {
    checkParamIndex(param);
    return ParameterMetaData.parameterNullableUnknown;
  }

  /**
   * {@inheritDoc} KaiwuDB doesn't have unsigned numbers
   */
  @Override
  public boolean isSigned(int param) throws SQLException {
    checkParamIndex(param);
    return connection.getTypeInfo().isSigned(oids[param - 1]);
  }

  private void checkParamIndex(int param) throws KSQLException {
    if (param < 1 || param > oids.length) {
      throw new KSQLException(
          GT.tr("The parameter index is out of range: {0}, number of parameters: {1}.",
              param, oids.length),
          KSQLState.INVALID_PARAMETER_VALUE);
    }
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isAssignableFrom(getClass());
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isAssignableFrom(getClass())) {
      return iface.cast(this);
    }
    throw new SQLException("Cannot unwrap to " + iface.getName());
  }
}
