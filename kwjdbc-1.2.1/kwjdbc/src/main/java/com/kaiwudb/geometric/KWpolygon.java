/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.geometric;

import com.kaiwudb.util.KWobject;
import com.kaiwudb.util.KWtokenizer;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * This implements the polygon datatype within KaiwuDB.
 */
public class KWpolygon extends KWobject implements Serializable, Cloneable {
  /**
   * The points defining the polygon.
   */
  public KWpoint[] points;

  /**
   * Creates a polygon using an array of KWpoints.
   *
   * @param points the points defining the polygon
   */
  public KWpolygon(KWpoint[] points) {
    this();
    this.points = points;
  }

  /**
   * @param s definition of the polygon in KaiwuDB's syntax.
   * @throws SQLException on conversion failure
   */
  public KWpolygon(String s) throws SQLException {
    this();
    setValue(s);
  }

  /**
   * Required by the driver.
   */
  public KWpolygon() {
    setType("polygon");
  }

  /**
   * @param s Definition of the polygon in KaiwuDB's syntax
   * @throws SQLException on conversion failure
   */
  @Override
  public void setValue(String s) throws SQLException {
    KWtokenizer t = new KWtokenizer(KWtokenizer.removePara(s), ',');
    int npoints = t.getSize();
    points = new KWpoint[npoints];
    for (int p = 0; p < npoints; p++) {
      points[p] = new KWpoint(t.getToken(p));
    }
  }

  /**
   * @param obj Object to compare with
   * @return true if the two polygons are identical
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KWpolygon) {
      KWpolygon p = (KWpolygon) obj;

      if (p.points.length != points.length) {
        return false;
      }

      for (int i = 0; i < points.length; i++) {
        if (!points[i].equals(p.points[i])) {
          return false;
        }
      }

      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    // XXX not very good..
    int hash = 0;
    for (int i = 0; i < points.length && i < 5; ++i) {
      hash = hash ^ points[i].hashCode();
    }
    return hash;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    KWpolygon newKWpolygon = (KWpolygon) super.clone();
    if (newKWpolygon.points != null) {
      newKWpolygon.points = (KWpoint[]) newKWpolygon.points.clone();
      for (int i = 0; i < newKWpolygon.points.length; ++i) {
        if (newKWpolygon.points[i] != null) {
          newKWpolygon.points[i] = (KWpoint) newKWpolygon.points[i].clone();
        }
      }
    }
    return newKWpolygon;
  }

  /**
   * @return the KWpolygon in the syntax expected by com.kaiwudb
   */
  @Override
  public String getValue() {
    StringBuilder b = new StringBuilder();
    b.append("(");
    for (int p = 0; p < points.length; p++) {
      if (p > 0) {
        b.append(",");
      }
      b.append(points[p].toString());
    }
    b.append(")");
    return b.toString();
  }
}
