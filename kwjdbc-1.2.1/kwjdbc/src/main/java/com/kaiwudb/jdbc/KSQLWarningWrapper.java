/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import java.sql.SQLWarning;

/**
 * Wrapper class for SQLWarnings that provides an optimisation to add
 * new warnings to the tail of the SQLWarning singly linked list, avoiding Θ(n) insertion time
 * of calling #setNextWarning on the head. By encapsulating this into a single object it allows
 * users(ie KwStatement) to atomically set and clear the warning chain.
 */
class KSQLWarningWrapper {

  private final SQLWarning firstWarning;
  private SQLWarning lastWarning;

  KSQLWarningWrapper(SQLWarning warning) {
    firstWarning = warning;
    lastWarning = warning;
  }

  void addWarning(SQLWarning sqlWarning) {
    lastWarning.setNextWarning(sqlWarning);
    lastWarning = sqlWarning;
  }

  SQLWarning getFirstWarning() {
    return firstWarning;
  }

}
