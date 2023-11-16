/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.geometric;

import com.kaiwudb.util.GT;
import com.kaiwudb.util.KWobject;
import com.kaiwudb.util.KWtokenizer;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * This implements a path (a multiple segmented line, which may be closed).
 */
public class KWpath extends KWobject implements Serializable, Cloneable {
  /**
   * True if the path is open, false if closed.
   */
  public boolean open;

  /**
   * The points defining this path.
   */
  public KWpoint[] points;

  /**
   * @param points the KWpoints that define the path
   * @param open True if the path is open, false if closed
   */
  public KWpath(KWpoint[] points, boolean open) {
    this();
    this.points = points;
    this.open = open;
  }

  /**
   * Required by the driver.
   */
  public KWpath() {
    setType("path");
  }

  /**
   * @param s definition of the path in KaiwuDB's syntax.
   * @throws SQLException on conversion failure
   */
  public KWpath(String s) throws SQLException {
    this();
    setValue(s);
  }

  /**
   * @param s Definition of the path in KaiwuDB's syntax
   * @throws SQLException on conversion failure
   */
  @Override
  public void setValue(String s) throws SQLException {
    // First test to see if were open
    if (s.startsWith("[") && s.endsWith("]")) {
      open = true;
      s = KWtokenizer.removeBox(s);
    } else if (s.startsWith("(") && s.endsWith(")")) {
      open = false;
      s = KWtokenizer.removePara(s);
    } else {
      throw new KSQLException(GT.tr("Cannot tell if path is open or closed: {0}.", s),
          KSQLState.DATA_TYPE_MISMATCH);
    }

    KWtokenizer t = new KWtokenizer(s, ',');
    int npoints = t.getSize();
    points = new KWpoint[npoints];
    for (int p = 0; p < npoints; p++) {
      points[p] = new KWpoint(t.getToken(p));
    }
  }

  /**
   * @param obj Object to compare with
   * @return true if the two paths are identical
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KWpath) {
      KWpath p = (KWpath) obj;

      if (p.points.length != points.length) {
        return false;
      }

      if (p.open != open) {
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
    KWpath newKWpath = (KWpath) super.clone();
    if (newKWpath.points != null) {
      newKWpath.points = (KWpoint[]) newKWpath.points.clone();
      for (int i = 0; i < newKWpath.points.length; ++i) {
        newKWpath.points[i] = (KWpoint) newKWpath.points[i].clone();
      }
    }
    return newKWpath;
  }

  /**
   * This returns the path in the syntax expected by com.kaiwudb.
   */
  @Override
  public String getValue() {
    StringBuilder b = new StringBuilder(open ? "[" : "(");

    for (int p = 0; p < points.length; p++) {
      if (p > 0) {
        b.append(",");
      }
      b.append(points[p].toString());
    }
    b.append(open ? "]" : ")");

    return b.toString();
  }

  public boolean isOpen() {
    return open;
  }

  public boolean isClosed() {
    return !open;
  }

  public void closePath() {
    open = false;
  }

  public void openPath() {
    open = true;
  }
}
