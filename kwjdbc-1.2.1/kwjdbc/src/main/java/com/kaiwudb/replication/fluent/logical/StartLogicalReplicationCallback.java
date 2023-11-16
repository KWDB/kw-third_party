/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent.logical;

import com.kaiwudb.replication.KWReplicationStream;

import java.sql.SQLException;

public interface StartLogicalReplicationCallback {
  KWReplicationStream start(LogicalReplicationOptions options) throws SQLException;
}
