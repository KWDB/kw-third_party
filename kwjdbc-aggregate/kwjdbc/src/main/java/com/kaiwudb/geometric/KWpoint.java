/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.geometric;

import com.kaiwudb.util.ByteConverter;
import com.kaiwudb.util.GT;
import com.kaiwudb.util.KWBinaryObject;
import com.kaiwudb.util.KWobject;
import com.kaiwudb.util.KWtokenizer;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.awt.Point;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * <p>It maps to the point datatype in com.kaiwudb.</p>
 *
 * <p>This implements a version of java.awt.Point, except it uses double to represent the coordinates.</p>
 */
public class KWpoint extends KWobject implements KWBinaryObject, Serializable, Cloneable {
  /**
   * The X coordinate of the point.
   */
  public double x;

  /**
   * The Y coordinate of the point.
   */
  public double y;

  /**
   * @param x coordinate
   * @param y coordinate
   */
  public KWpoint(double x, double y) {
    this();
    this.x = x;
    this.y = y;
  }

  /**
   * This is called mainly from the other geometric types, when a point is embedded within their
   * definition.
   *
   * @param value Definition of this point in KaiwuDB's syntax
   * @throws SQLException if something goes wrong
   */
  public KWpoint(String value) throws SQLException {
    this();
    setValue(value);
  }

  /**
   * Required by the driver.
   */
  public KWpoint() {
    setType("point");
  }

  /**
   * @param s Definition of this point in KaiwuDB's syntax
   * @throws SQLException on conversion failure
   */
  @Override
  public void setValue(String s) throws SQLException {
    KWtokenizer t = new KWtokenizer(KWtokenizer.removePara(s), ',');
    try {
      x = Double.parseDouble(t.getToken(0));
      y = Double.parseDouble(t.getToken(1));
    } catch (NumberFormatException e) {
      throw new KSQLException(GT.tr("Conversion to type {0} failed: {1}.", type, s),
          KSQLState.DATA_TYPE_MISMATCH, e);
    }
  }

  /**
   * @param b Definition of this point in KaiwuDB's binary syntax
   */
  @Override
  public void setByteValue(byte[] b, int offset) {
    x = ByteConverter.float8(b, offset);
    y = ByteConverter.float8(b, offset + 8);
  }

  /**
   * @param obj Object to compare with
   * @return true if the two points are identical
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KWpoint) {
      KWpoint p = (KWpoint) obj;
      return x == p.x && y == p.y;
    }
    return false;
  }

  @Override
  public int hashCode() {
    long v1 = Double.doubleToLongBits(x);
    long v2 = Double.doubleToLongBits(y);
    return (int) (v1 ^ v2 ^ (v1 >>> 32) ^ (v2 >>> 32));
  }

  /**
   * @return the KWpoint in the syntax expected by com.kaiwudb
   */
  @Override
  public String getValue() {
    return "(" + x + "," + y + ")";
  }

  @Override
  public int lengthInBytes() {
    return 16;
  }

  /**
   * Populate the byte array with KWpoint in the binary syntax expected by com.kaiwudb.
   */
  @Override
  public void toBytes(byte[] b, int offset) {
    ByteConverter.float8(b, offset, x);
    ByteConverter.float8(b, offset + 8, y);
  }

  /**
   * Translate the point by the supplied amount.
   *
   * @param x integer amount to add on the x axis
   * @param y integer amount to add on the y axis
   */
  public void translate(int x, int y) {
    translate((double) x, (double) y);
  }

  /**
   * Translate the point by the supplied amount.
   *
   * @param x double amount to add on the x axis
   * @param y double amount to add on the y axis
   */
  public void translate(double x, double y) {
    this.x += x;
    this.y += y;
  }

  /**
   * Moves the point to the supplied coordinates.
   *
   * @param x integer coordinate
   * @param y integer coordinate
   */
  public void move(int x, int y) {
    setLocation(x, y);
  }

  /**
   * Moves the point to the supplied coordinates.
   *
   * @param x double coordinate
   * @param y double coordinate
   */
  public void move(double x, double y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Moves the point to the supplied coordinates. refer to java.awt.Point for description of this.
   *
   * @param x integer coordinate
   * @param y integer coordinate
   * @see java.awt.Point
   */
  public void setLocation(int x, int y) {
    move((double) x, (double) y);
  }

  /**
   * Moves the point to the supplied java.awt.Point refer to java.awt.Point for description of this.
   *
   * @param p Point to move to
   * @see java.awt.Point
   */
  public void setLocation(Point p) {
    setLocation(p.x, p.y);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    // squid:S2157 "Cloneables" should implement "clone
    return super.clone();
  }
}
