/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

public interface Version {

  /**
   * Get a machine-readable version number.
   *
   * @return the version in numeric XXYYZZ form, e.g. 90401 for 9.4.1
   */
  int getVersionNum();

}
