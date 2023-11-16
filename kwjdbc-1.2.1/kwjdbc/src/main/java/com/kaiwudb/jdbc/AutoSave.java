/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

public enum AutoSave {
  NEVER,
  ALWAYS,
  CONSERVATIVE;

  private final String value;

  AutoSave() {
    value = this.name().toLowerCase();
  }

  public String value() {
    return value;
  }

  public static AutoSave of(String value) {
    return valueOf(value.toUpperCase());
  }
}
