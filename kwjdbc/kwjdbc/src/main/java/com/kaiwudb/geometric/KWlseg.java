/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.geometric;

import com.kaiwudb.util.GT;
import com.kaiwudb.util.KWobject;
import com.kaiwudb.util.KWtokenizer;
import com.kaiwudb.util.KSQLState;
import com.kaiwudb.util.KSQLException;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * This implements a lseg (line segment) consisting of two points.
 */
public class KWlseg extends KWobject implements Serializable, Cloneable {
  /**
   * These are the two points.
   */
  public KWpoint[] point = new KWpoint[2];

  /**
   * @param x1 coordinate for first point
   * @param y1 coordinate for first point
   * @param x2 coordinate for second point
   * @param y2 coordinate for second point
   */
  public KWlseg(double x1, double y1, double x2, double y2) {
    this(new KWpoint(x1, y1), new KWpoint(x2, y2));
  }

  /**
   * @param p1 first point
   * @param p2 second point
   */
  public KWlseg(KWpoint p1, KWpoint p2) {
    this();
    this.point[0] = p1;
    this.point[1] = p2;
  }

  /**
   * @param s definition of the line segment in KaiwuDB's syntax.
   * @throws SQLException on conversion failure
   */
  public KWlseg(String s) throws SQLException {
    this();
    setValue(s);
  }

  /**
   * required by the driver.
   */
  public KWlseg() {
    setType("lseg");
  }

  /**
   * @param s Definition of the line segment in KaiwuDB's syntax
   * @throws SQLException on conversion failure
   */
  @Override
  public void setValue(String s) throws SQLException {
    KWtokenizer t = new KWtokenizer(KWtokenizer.removeBox(s), ',');
    if (t.getSize() != 2) {
      throw new KSQLException(GT.tr("Conversion to type {0} failed: {1}.", type, s),
          KSQLState.DATA_TYPE_MISMATCH);
    }

    point[0] = new KWpoint(t.getToken(0));
    point[1] = new KWpoint(t.getToken(1));
  }

  /**
   * @param obj Object to compare with
   * @return true if the two line segments are identical
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KWlseg) {
      KWlseg p = (KWlseg) obj;
      return (p.point[0].equals(point[0]) && p.point[1].equals(point[1]))
          || (p.point[0].equals(point[1]) && p.point[1].equals(point[0]));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return point[0].hashCode() ^ point[1].hashCode();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    KWlseg newKWlseg = (KWlseg) super.clone();
    if (newKWlseg.point != null) {
      newKWlseg.point = (KWpoint[]) newKWlseg.point.clone();
      for (int i = 0; i < newKWlseg.point.length; ++i) {
        if (newKWlseg.point[i] != null) {
          newKWlseg.point[i] = (KWpoint) newKWlseg.point[i].clone();
        }
      }
    }
    return newKWlseg;
  }

  /**
   * @return the KWlseg in the syntax expected by com.kaiwudb
   */
  @Override
  public String getValue() {
    return "[" + point[0] + "," + point[1] + "]";
  }
}
