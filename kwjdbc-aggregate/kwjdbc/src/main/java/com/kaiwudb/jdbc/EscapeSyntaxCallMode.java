/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import com.kaiwudb.KWProperty;

/**
 * <p>Specifies whether a SELECT/CALL statement is used for the underlying SQL for JDBC escape call syntax: 'select' means to
 * always use SELECT, 'callIfNoReturn' means to use CALL if there is no return parameter (otherwise use SELECT), and 'call' means
 * to always use CALL.</p>
 *
 * @see KWProperty#ESCAPE_SYNTAX_CALL_MODE
 */
public enum EscapeSyntaxCallMode {
  SELECT("select"),
  CALL_IF_NO_RETURN("callIfNoReturn"),
  CALL("call");

  private final String value;

  EscapeSyntaxCallMode(String value) {
    this.value = value;
  }

  public static EscapeSyntaxCallMode of(String mode) {
    for (EscapeSyntaxCallMode escapeSyntaxCallMode : values()) {
      if (escapeSyntaxCallMode.value.equals(mode)) {
        return escapeSyntaxCallMode;
      }
    }
    return SELECT;
  }

  public String value() {
    return value;
  }
}
