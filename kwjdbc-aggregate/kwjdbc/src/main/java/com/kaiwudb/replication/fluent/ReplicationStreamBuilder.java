/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent;

import com.kaiwudb.replication.fluent.logical.ChainedLogicalStreamBuilder;
import com.kaiwudb.replication.fluent.logical.LogicalReplicationOptions;
import com.kaiwudb.replication.fluent.logical.LogicalStreamBuilder;
import com.kaiwudb.replication.fluent.logical.StartLogicalReplicationCallback;
import com.kaiwudb.replication.fluent.physical.ChainedPhysicalStreamBuilder;
import com.kaiwudb.replication.fluent.physical.PhysicalReplicationOptions;
import com.kaiwudb.replication.fluent.physical.PhysicalStreamBuilder;
import com.kaiwudb.replication.fluent.physical.StartPhysicalReplicationCallback;
import com.kaiwudb.core.BaseConnection;
import com.kaiwudb.core.ReplicationProtocol;
import com.kaiwudb.replication.KWReplicationStream;

import java.sql.SQLException;

public class ReplicationStreamBuilder implements ChainedStreamBuilder {
  private final BaseConnection baseConnection;

  /**
   * @param connection not null connection with that will be associate replication
   */
  public ReplicationStreamBuilder(final BaseConnection connection) {
    this.baseConnection = connection;
  }

  @Override
  public ChainedLogicalStreamBuilder logical() {
    return new LogicalStreamBuilder(new StartLogicalReplicationCallback() {
      @Override
      public KWReplicationStream start(LogicalReplicationOptions options) throws SQLException {
        ReplicationProtocol protocol = baseConnection.getReplicationProtocol();
        return protocol.startLogical(options);
      }
    });
  }

  @Override
  public ChainedPhysicalStreamBuilder physical() {
    return new PhysicalStreamBuilder(new StartPhysicalReplicationCallback() {
      @Override
      public KWReplicationStream start(PhysicalReplicationOptions options) throws SQLException {
        ReplicationProtocol protocol = baseConnection.getReplicationProtocol();
        return protocol.startPhysical(options);
      }
    });
  }
}
