/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent.physical;

import com.kaiwudb.replication.KWReplicationStream;
import com.kaiwudb.replication.fluent.ChainedCommonStreamBuilder;

import java.sql.SQLException;

public interface ChainedPhysicalStreamBuilder extends
    ChainedCommonStreamBuilder<ChainedPhysicalStreamBuilder> {

  /**
   * Open physical replication stream.
   *
   * @return not null KWReplicationStream available for fetch wal logs in binary form
   * @throws SQLException on error
   */
  KWReplicationStream start() throws SQLException;
}
