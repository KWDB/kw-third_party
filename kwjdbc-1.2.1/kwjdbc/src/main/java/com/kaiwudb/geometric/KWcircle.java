/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.geometric;

import com.kaiwudb.util.GT;
import com.kaiwudb.util.KWtokenizer;
import com.kaiwudb.util.KSQLState;
import com.kaiwudb.util.KWobject;
import com.kaiwudb.util.KSQLException;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * This represents KaiwuDB's circle datatype, consisting of a point and a radius.
 */
public class KWcircle extends KWobject implements Serializable, Cloneable {
  /**
   * This is the center point.
   */
  public KWpoint center;

  /**
   * This is the radius.
   */
  public double radius;

  /**
   * @param x coordinate of center
   * @param y coordinate of center
   * @param r radius of circle
   */
  public KWcircle(double x, double y, double r) {
    this(new KWpoint(x, y), r);
  }

  /**
   * @param c KWpoint describing the circle's center
   * @param r radius of circle
   */
  public KWcircle(KWpoint c, double r) {
    this();
    this.center = c;
    this.radius = r;
  }

  /**
   * @param s definition of the circle in KaiwuDB's syntax.
   * @throws SQLException on conversion failure
   */
  public KWcircle(String s) throws SQLException {
    this();
    setValue(s);
  }

  /**
   * This constructor is used by the driver.
   */
  public KWcircle() {
    setType("circle");
  }

  /**
   * @param s definition of the circle in KaiwuDB's syntax.
   * @throws SQLException on conversion failure
   */
  @Override
  public void setValue(String s) throws SQLException {
    KWtokenizer t = new KWtokenizer(KWtokenizer.removeAngle(s), ',');
    if (t.getSize() != 2) {
      throw new KSQLException(GT.tr("Conversion to type {0} failed: {1}.", type, s),
          KSQLState.DATA_TYPE_MISMATCH);
    }

    try {
      center = new KWpoint(t.getToken(0));
      radius = Double.parseDouble(t.getToken(1));
    } catch (NumberFormatException e) {
      throw new KSQLException(GT.tr("Conversion to type {0} failed: {1}.", type, s),
          KSQLState.DATA_TYPE_MISMATCH, e);
    }
  }

  /**
   * @param obj Object to compare with
   * @return true if the two circles are identical
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KWcircle) {
      KWcircle p = (KWcircle) obj;
      return p.center.equals(center) && p.radius == radius;
    }
    return false;
  }

  @Override
  public int hashCode() {
    long v = Double.doubleToLongBits(radius);
    return (int) (center.hashCode() ^ v ^ (v >>> 32));
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    KWcircle newKWcircle = (KWcircle) super.clone();
    if (newKWcircle.center != null) {
      newKWcircle.center = (KWpoint) newKWcircle.center.clone();
    }
    return newKWcircle;
  }

  /**
   * @return the KWcircle in the syntax expected by com.kaiwudb
   */
  @Override
  public String getValue() {
    return "<" + center + "," + radius + ">";
  }
}
