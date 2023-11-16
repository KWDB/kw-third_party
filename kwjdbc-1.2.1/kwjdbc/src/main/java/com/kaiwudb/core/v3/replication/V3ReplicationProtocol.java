/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core.v3.replication;

import com.kaiwudb.replication.KWReplicationStream;
import com.kaiwudb.replication.ReplicationType;
import com.kaiwudb.replication.fluent.CommonOptions;
import com.kaiwudb.replication.fluent.logical.LogicalReplicationOptions;
import com.kaiwudb.replication.fluent.physical.PhysicalReplicationOptions;
import com.kaiwudb.copy.CopyDual;
import com.kaiwudb.core.KWStream;
import com.kaiwudb.core.QueryExecutor;
import com.kaiwudb.core.ReplicationProtocol;
import com.kaiwudb.util.GT;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class V3ReplicationProtocol implements ReplicationProtocol {

  private static final Logger LOGGER = Logger.getLogger(V3ReplicationProtocol.class.getName());
  private final QueryExecutor queryExecutor;
  private final KWStream kwStream;

  public V3ReplicationProtocol(QueryExecutor queryExecutor, KWStream kwStream) {
    this.queryExecutor = queryExecutor;
    this.kwStream = kwStream;
  }

  @Override
  public KWReplicationStream startLogical(LogicalReplicationOptions options)
      throws SQLException {

    String query = createStartLogicalQuery(options);
    return initializeReplication(query, options, ReplicationType.LOGICAL);
  }

  @Override
  public KWReplicationStream startPhysical(PhysicalReplicationOptions options)
      throws SQLException {

    String query = createStartPhysicalQuery(options);
    return initializeReplication(query, options, ReplicationType.PHYSICAL);
  }

  private KWReplicationStream initializeReplication(String query, CommonOptions options,
                                                    ReplicationType replicationType)
      throws SQLException {
    LOGGER.log(Level.FINEST, " FE=> StartReplication(query: {0})", query);

    configureSocketTimeout(options);
    CopyDual copyDual = (CopyDual) queryExecutor.startCopy(query, true);

    return new V3KWReplicationStream(
        copyDual,
        options.getStartLSNPosition(),
        options.getStatusInterval(),
        replicationType
    );
  }

  /**
   * START_REPLICATION [SLOT slot_name] [PHYSICAL] XXX/XXX.
   */
  private String createStartPhysicalQuery(PhysicalReplicationOptions options) {
    StringBuilder builder = new StringBuilder();
    builder.append("START_REPLICATION");

    if (options.getSlotName() != null) {
      builder.append(" SLOT ").append(options.getSlotName());
    }

    builder.append(" PHYSICAL ").append(options.getStartLSNPosition().asString());

    return builder.toString();
  }

  /**
   * START_REPLICATION SLOT slot_name LOGICAL XXX/XXX [ ( option_name [option_value] [, ... ] ) ]
   */
  private String createStartLogicalQuery(LogicalReplicationOptions options) {
    StringBuilder builder = new StringBuilder();
    builder.append("START_REPLICATION SLOT ")
        .append(options.getSlotName())
        .append(" LOGICAL ")
        .append(options.getStartLSNPosition().asString());

    Properties slotOptions = options.getSlotOptions();
    if (slotOptions.isEmpty()) {
      return builder.toString();
    }

    //todo replace on java 8
    builder.append(" (");
    boolean isFirst = true;
    for (String name : slotOptions.stringPropertyNames()) {
      if (isFirst) {
        isFirst = false;
      } else {
        builder.append(", ");
      }
      builder.append('\"').append(name).append('\"').append(" ")
          .append('\'').append(slotOptions.getProperty(name)).append('\'');
    }
    builder.append(")");

    return builder.toString();
  }

  private void configureSocketTimeout(CommonOptions options) throws KSQLException {
    if (options.getStatusInterval() == 0) {
      return;
    }

    try {
      int previousTimeOut = kwStream.getSocket().getSoTimeout();

      int minimalTimeOut;
      if (previousTimeOut > 0) {
        minimalTimeOut = Math.min(previousTimeOut, options.getStatusInterval());
      } else {
        minimalTimeOut = options.getStatusInterval();
      }

      kwStream.getSocket().setSoTimeout(minimalTimeOut);
      // Use blocking 1ms reads for `available()` checks
      kwStream.setMinStreamAvailableCheckDelay(0);
    } catch (IOException ioe) {
      throw new KSQLException(GT.tr("The connection attempt failed."),
          KSQLState.CONNECTION_UNABLE_TO_CONNECT, ioe);
    }
  }

}
