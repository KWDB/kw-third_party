/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.geometric;

import com.kaiwudb.util.GT;
import com.kaiwudb.util.KWBinaryObject;
import com.kaiwudb.util.KWtokenizer;
import com.kaiwudb.util.KWobject;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * This represents the box datatype within com.kaiwudb.
 */
public class KWbox extends KWobject implements KWBinaryObject, Serializable, Cloneable {
  /**
   * These are the two points.
   */
  public KWpoint[] point = new KWpoint[2];

  /**
   * @param x1 first x coordinate
   * @param y1 first y coordinate
   * @param x2 second x coordinate
   * @param y2 second y coordinate
   */
  public KWbox(double x1, double y1, double x2, double y2) {
    this();
    this.point[0] = new KWpoint(x1, y1);
    this.point[1] = new KWpoint(x2, y2);
  }

  /**
   * @param p1 first point
   * @param p2 second point
   */
  public KWbox(KWpoint p1, KWpoint p2) {
    this();
    this.point[0] = p1;
    this.point[1] = p2;
  }

  /**
   * @param s Box definition in KaiwuDB syntax
   * @throws SQLException if definition is invalid
   */
  public KWbox(String s) throws SQLException {
    this();
    setValue(s);
  }

  /**
   * Required constructor.
   */
  public KWbox() {
    setType("box");
  }

  /**
   * This method sets the value of this object. It should be overidden, but still called by
   * subclasses.
   *
   * @param value a string representation of the value of the object
   * @throws SQLException thrown if value is invalid for this type
   */
  @Override
  public void setValue(String value) throws SQLException {
    KWtokenizer t = new KWtokenizer(value, ',');
    if (t.getSize() != 2) {
      throw new KSQLException(
          GT.tr("Conversion to type {0} failed: {1}.", type, value),
          KSQLState.DATA_TYPE_MISMATCH);
    }

    point[0] = new KWpoint(t.getToken(0));
    point[1] = new KWpoint(t.getToken(1));
  }

  /**
   * @param b Definition of this point in KaiwuDB's binary syntax
   */
  @Override
  public void setByteValue(byte[] b, int offset) {
    point[0] = new KWpoint();
    point[0].setByteValue(b, offset);
    point[1] = new KWpoint();
    point[1].setByteValue(b, offset + point[0].lengthInBytes());
  }

  /**
   * @param obj Object to compare with
   * @return true if the two boxes are identical
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KWbox) {
      KWbox p = (KWbox) obj;

      // Same points.
      if (p.point[0].equals(point[0]) && p.point[1].equals(point[1])) {
        return true;
      }

      // Points swapped.
      if (p.point[0].equals(point[1]) && p.point[1].equals(point[0])) {
        return true;
      }

      // Using the opposite two points of the box:
      // (x1,y1),(x2,y2) -> (x1,y2),(x2,y1)
      if (p.point[0].x == point[0].x && p.point[0].y == point[1].y
          && p.point[1].x == point[1].x && p.point[1].y == point[0].y) {
        return true;
      }

      // Using the opposite two points of the box, and the points are swapped
      // (x1,y1),(x2,y2) -> (x2,y1),(x1,y2)
      if (p.point[0].x == point[1].x && p.point[0].y == point[0].y
          && p.point[1].x == point[0].x && p.point[1].y == point[1].y) {
        return true;
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    // This relies on the behaviour of point's hashcode being an exclusive-OR of
    // its X and Y components; we end up with an exclusive-OR of the two X and
    // two Y components, which is equal whenever equals() would return true
    // since xor is commutative.
    return point[0].hashCode() ^ point[1].hashCode();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    KWbox newKWbox = (KWbox) super.clone();
    if (newKWbox.point != null) {
      newKWbox.point = newKWbox.point.clone();
      for (int i = 0; i < newKWbox.point.length; ++i) {
        if (newKWbox.point[i] != null) {
          newKWbox.point[i] = (KWpoint) newKWbox.point[i].clone();
        }
      }
    }
    return newKWbox;
  }

  /**
   * @return the KWbox in the syntax expected by com.kaiwudb
   */
  @Override
  public String getValue() {
    return point[0].toString() + "," + point[1].toString();
  }

  @Override
  public int lengthInBytes() {
    return point[0].lengthInBytes() + point[1].lengthInBytes();
  }

  @Override
  public void toBytes(byte[] bytes, int offset) {
    point[0].toBytes(bytes, offset);
    point[1].toBytes(bytes, offset + point[0].lengthInBytes());
  }
}
