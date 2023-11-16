/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

/**
 * Abstraction of a cursor over a returned resultset. This is an opaque interface that only provides
 * a way to close the cursor; all other operations are done by passing a ResultCursor to
 * QueryExecutor methods.
 *
 * @author Oliver Jowett (oliver@opencloud.com)
 */
public interface ResultCursor {
  /**
   * Close this cursor. This may not immediately free underlying resources but may make it happen
   * more promptly. Closed cursors should not be passed to QueryExecutor methods.
   */
  void close();
}
