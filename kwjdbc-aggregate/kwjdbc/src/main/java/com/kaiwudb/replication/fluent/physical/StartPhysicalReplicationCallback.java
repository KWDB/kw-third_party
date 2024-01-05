/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent.physical;

import com.kaiwudb.replication.KWReplicationStream;

import java.sql.SQLException;

public interface StartPhysicalReplicationCallback {
  KWReplicationStream start(PhysicalReplicationOptions options) throws SQLException;
}
