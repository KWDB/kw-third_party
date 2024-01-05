/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.util;

import java.sql.SQLException;

public class KSQLException extends SQLException {

  private ServerErrorMessage serverError;

  public KSQLException(String msg, KSQLState state, Throwable cause) {
    super(msg, state == null ? null : state.getState(), cause);
  }

  public KSQLException(String msg, KSQLState state) {
    super(msg, state == null ? null : state.getState());
  }

  public KSQLException(ServerErrorMessage serverError) {
    this(serverError, true);
  }

  public KSQLException(ServerErrorMessage serverError, boolean detail) {
    super(detail ? serverError.toString() : serverError.getNonSensitiveErrorMessage(), serverError.getSQLState());
    this.serverError = serverError;
  }

  public ServerErrorMessage getServerErrorMessage() {
    return serverError;
  }
}
