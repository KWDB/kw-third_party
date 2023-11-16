/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent;

import com.kaiwudb.replication.fluent.logical.ChainedLogicalCreateSlotBuilder;
import com.kaiwudb.replication.fluent.logical.LogicalCreateSlotBuilder;
import com.kaiwudb.replication.fluent.physical.ChainedPhysicalCreateSlotBuilder;
import com.kaiwudb.replication.fluent.physical.PhysicalCreateSlotBuilder;
import com.kaiwudb.core.BaseConnection;

public class ReplicationCreateSlotBuilder implements ChainedCreateReplicationSlotBuilder {
  private final BaseConnection baseConnection;

  public ReplicationCreateSlotBuilder(BaseConnection baseConnection) {
    this.baseConnection = baseConnection;
  }

  @Override
  public ChainedLogicalCreateSlotBuilder logical() {
    return new LogicalCreateSlotBuilder(baseConnection);
  }

  @Override
  public ChainedPhysicalCreateSlotBuilder physical() {
    return new PhysicalCreateSlotBuilder(baseConnection);
  }
}
