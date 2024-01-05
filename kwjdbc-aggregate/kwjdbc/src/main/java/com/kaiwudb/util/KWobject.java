/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.util;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * KWobject is a class used to describe unknown types An unknown type is any type that is unknown by
 * JDBC Standards.
 */
public class KWobject implements Serializable, Cloneable {
  protected String type;
  protected String value;

  /**
   * This is called by com.kaiwudb.Connection.getObject() to create the object.
   */
  public KWobject() {
  }

  /**
   * <p>This method sets the type of this object.</p>
   *
   * <p>It should not be extended by subclasses, hence it is final</p>
   *
   * @param type a string describing the type of the object
   */
  public final void setType(String type) {
    this.type = type;
  }

  /**
   * This method sets the value of this object. It must be overridden.
   *
   * @param value a string representation of the value of the object
   * @throws SQLException thrown if value is invalid for this type
   */

  public void setValue(String value) throws SQLException {
    this.value = value;
  }

  /**
   * As this cannot change during the life of the object, it's final.
   *
   * @return the type name of this object
   */
  public final String getType() {
    return type;
  }

  /**
   * This must be overidden, to return the value of the object, in the form required by com.kaiwudb.
   *
   * @return the value of this object
   */
  public String getValue() {
    return value;
  }

  /**
   * This must be overidden to allow comparisons of objects.
   *
   * @param obj Object to compare with
   * @return true if the two boxes are identical
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof KWobject) {
      final Object otherValue = ((KWobject) obj).getValue();

      if (otherValue == null) {
        return getValue() == null;
      }
      return otherValue.equals(getValue());
    }
    return false;
  }

  /**
   * This must be overidden to allow the object to be cloned.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /**
   * This is defined here, so user code need not overide it.
   *
   * @return the value of this object, in the syntax expected by com.kaiwudb
   */
  @Override
  public String toString() {
    return getValue();
  }

  /**
   * Compute hash. As equals() use only value. Return the same hash for the same value.
   *
   * @return Value hashcode, 0 if value is null {@link java.util.Objects#hashCode(Object)}
   */
  @Override
  public int hashCode() {
    return getValue() != null ? getValue().hashCode() : 0;
  }
}
