/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
// Copyright (c) 2004, Open Cloud Limited.

package com.kaiwudb.core.v3;

import com.kaiwudb.util.GT;
import com.kaiwudb.core.ParameterList;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * Parameter list for V3 query strings that contain multiple statements. We delegate to one
 * SimpleParameterList per statement, and translate parameter indexes as needed.
 *
 * @author Oliver Jowett (oliver@opencloud.com)
 */
class CompositeParameterList implements V3ParameterList {
  CompositeParameterList(SimpleParameterList[] subparams, int[] offsets) {
    this.subparams = subparams;
    this.offsets = offsets;
    this.total = offsets[offsets.length - 1] + subparams[offsets.length - 1].getInParameterCount();
  }

  private int findSubParam(int index) throws SQLException {
    if (index < 1 || index > total) {
      throw new KSQLException(
          GT.tr("The column index is out of range: {0}, number of columns: {1}.", index, total),
          KSQLState.INVALID_PARAMETER_VALUE);
    }

    for (int i = offsets.length - 1; i >= 0; --i) {
      if (offsets[i] < index) {
        return i;
      }
    }

    throw new IllegalArgumentException("I am confused; can't find a subparam for index " + index);
  }

  @Override
  public void registerOutParameter(int index, int sqlType) {

  }

  public int getDirection(int i) {
    return 0;
  }

  @Override
  public int getParameterCount() {
    return total;
  }

  @Override
  public int getInParameterCount() {
    return total;
  }

  @Override
  public int getOutParameterCount() {
    return 0;
  }

  @Override
  public int[] getTypeOIDs() {
    int[] oids = new int[total];
    for (int i = 0; i < offsets.length; i++) {
      int[] subOids = subparams[i].getTypeOIDs();
      System.arraycopy(subOids, 0, oids, offsets[i], subOids.length);
    }
    return oids;
  }

  @Override
  public void setIntParameter(int index, int value) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setIntParameter(index - offsets[sub], value);
  }

  @Override
  public void setLiteralParameter(int index, String value, int oid) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setStringParameter(index - offsets[sub], value, oid);
  }

  @Override
  public void setStringParameter(int index, String value, int oid) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setStringParameter(index - offsets[sub], value, oid);
  }

  @Override
  public void setBinaryParameter(int index, byte[] value, int oid) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setBinaryParameter(index - offsets[sub], value, oid);
  }

  @Override
  public void setBytea(int index, byte[] data, int offset, int length) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setBytea(index - offsets[sub], data, offset, length);
  }

  @Override
  public void setBytea(int index, InputStream stream, int length) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setBytea(index - offsets[sub], stream, length);
  }

  @Override
  public void setBytea(int index, InputStream stream) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setBytea(index - offsets[sub], stream);
  }

  @Override
  public void setText(int index, InputStream stream) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setText(index - offsets[sub], stream);
  }

  @Override
  public void setNull(int index, int oid) throws SQLException {
    int sub = findSubParam(index);
    subparams[sub].setNull(index - offsets[sub], oid);
  }

  @Override
  public String toString(int index, boolean standardConformingStrings) {
    try {
      int sub = findSubParam(index);
      return subparams[sub].toString(index - offsets[sub], standardConformingStrings);
    } catch (SQLException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  @Override
  public ParameterList copy() {
    SimpleParameterList[] copySub = new SimpleParameterList[subparams.length];
    for (int sub = 0; sub < subparams.length; ++sub) {
      copySub[sub] = (SimpleParameterList) subparams[sub].copy();
    }

    return new CompositeParameterList(copySub, offsets);
  }

  @Override
  public void clear() {
    for (SimpleParameterList subparam : subparams) {
      subparam.clear();
    }
  }

  @Override
  public SimpleParameterList[] getSubparams() {
    return subparams;
  }

  @Override
  public void checkAllParametersSet() throws SQLException {
    for (SimpleParameterList subparam : subparams) {
      subparam.checkAllParametersSet();
    }
  }

  @Override
  public byte[][] getEncoding() {
    return null; // unsupported
  }

  @Override
  public byte[] getFlags() {
    return null; // unsupported
  }

  @Override
  public int[] getParamTypes() {
    return null; // unsupported
  }

  @Override
  public Object[] getValues() {
    return null; // unsupported
  }

  @Override
  public void appendAll(ParameterList list) throws SQLException {
    // no-op, unsupported
  }

  @Override
  public void convertFunctionOutParameters() {
    for (SimpleParameterList subparam : subparams) {
      subparam.convertFunctionOutParameters();
    }
  }

  private final int total;
  private final SimpleParameterList[] subparams;
  private final int[] offsets;
}
