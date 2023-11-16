/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.util;

import java.sql.SQLException;

/**
 * KWBinaryObject is a inteface that classes extending {@link KWobject} can use to take advantage of
 * more optimal binary encoding of the data type.
 */
public interface KWBinaryObject {
  /**
   * This method is called to set the value of this object.
   *
   * @param value data containing the binary representation of the value of the object
   * @param offset the offset in the byte array where object data starts
   * @throws SQLException thrown if value is invalid for this type
   */
  void setByteValue(byte[] value, int offset) throws SQLException;

  /**
   * This method is called to return the number of bytes needed to store this object in the binary
   * form required by com.kaiwudb.
   *
   * @return the number of bytes needed to store this object
   */
  int lengthInBytes();

  /**
   * This method is called the to store the value of the object, in the binary form required by com.kaiwudb.
   *
   * @param bytes the array to store the value, it is guaranteed to be at lest
   *        {@link #lengthInBytes} in size.
   * @param offset the offset in the byte array where object must be stored
   */
  void toBytes(byte[] bytes, int offset);
}
