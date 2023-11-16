/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent.physical;

import com.kaiwudb.replication.LogSequenceNumber;
import com.kaiwudb.replication.KWReplicationStream;
import com.kaiwudb.replication.fluent.AbstractStreamBuilder;

import java.sql.SQLException;

public class PhysicalStreamBuilder extends AbstractStreamBuilder<ChainedPhysicalStreamBuilder>
    implements ChainedPhysicalStreamBuilder, PhysicalReplicationOptions {

  private final StartPhysicalReplicationCallback startCallback;

  /**
   * @param startCallback not null callback that should be execute after build parameters for start
   *                      replication
   */
  public PhysicalStreamBuilder(StartPhysicalReplicationCallback startCallback) {
    this.startCallback = startCallback;
  }

  @Override
  protected ChainedPhysicalStreamBuilder self() {
    return this;
  }

  @Override
  public KWReplicationStream start() throws SQLException {
    return this.startCallback.start(this);
  }

  @Override
  public String getSlotName() {
    return slotName;
  }

  @Override
  public LogSequenceNumber getStartLSNPosition() {
    return startPosition;
  }

  @Override
  public int getStatusInterval() {
    return statusIntervalMs;
  }
}
