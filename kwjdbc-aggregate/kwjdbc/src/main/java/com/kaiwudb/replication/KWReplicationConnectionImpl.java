/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication;

import com.kaiwudb.replication.fluent.ChainedCreateReplicationSlotBuilder;
import com.kaiwudb.replication.fluent.ChainedStreamBuilder;
import com.kaiwudb.replication.fluent.ReplicationCreateSlotBuilder;
import com.kaiwudb.replication.fluent.ReplicationStreamBuilder;
import com.kaiwudb.core.BaseConnection;

import java.sql.SQLException;
import java.sql.Statement;

public class KWReplicationConnectionImpl implements KWReplicationConnection {
  private BaseConnection connection;

  public KWReplicationConnectionImpl(BaseConnection connection) {
    this.connection = connection;
  }

  @Override
  public ChainedStreamBuilder replicationStream() {
    return new ReplicationStreamBuilder(connection);
  }

  @Override
  public ChainedCreateReplicationSlotBuilder createReplicationSlot() {
    return new ReplicationCreateSlotBuilder(connection);
  }

  @Override
  public void dropReplicationSlot(String slotName) throws SQLException {
    if (slotName == null || slotName.isEmpty()) {
      throw new IllegalArgumentException("Replication slot name can't be null or empty");
    }

    Statement statement = connection.createStatement();
    try {
      statement.execute("DROP_REPLICATION_SLOT " + slotName);
    } finally {
      statement.close();
    }
  }
}
