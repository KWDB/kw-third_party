/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import com.kaiwudb.core.ParameterList;
import com.kaiwudb.core.ResultCursor;
import com.kaiwudb.core.Field;
import com.kaiwudb.core.Query;

import java.util.List;

class CallableBatchResultHandler extends BatchResultHandler {
  CallableBatchResultHandler(KwStatement statement, Query[] queries, ParameterList[] parameterLists) {
    super(statement, queries, parameterLists, false);
  }

  @Override
  public void handleResultRows(Query fromQuery, Field[] fields, List<byte[][]> tuples, ResultCursor cursor) {
    /* ignore */
  }
}
