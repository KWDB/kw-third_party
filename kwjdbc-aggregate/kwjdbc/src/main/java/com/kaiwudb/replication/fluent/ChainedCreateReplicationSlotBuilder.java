/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent;

import com.kaiwudb.replication.fluent.logical.ChainedLogicalCreateSlotBuilder;
import com.kaiwudb.replication.fluent.physical.ChainedPhysicalCreateSlotBuilder;

/**
 * Fluent interface for specify common parameters for Logical and Physical replication.
 */
public interface ChainedCreateReplicationSlotBuilder {
  /**
   * Get the logical slot builder.
   * Example usage:
   * <pre>
   *   {@code
   *
   *    kwConnection
   *        .getReplicationAPI()
   *        .createReplicationSlot()
   *        .logical()
   *        .withSlotName("mySlot")
   *        .withOutputPlugin("test_decoding")
   *        .make();
   *
   *    KWReplicationStream stream =
   *        kwConnection
   *            .getReplicationAPI()
   *            .replicationStream()
   *            .logical()
   *            .withSlotName("mySlot")
   *            .withSlotOption("include-xids", false)
   *            .withSlotOption("skip-empty-xacts", true)
   *            .start();
   *
   *    while (true) {
   *      ByteBuffer buffer = stream.read();
   *      //process logical changes
   *    }
   *
   *   }
   * </pre>
   * @return not null fluent api
   */
  ChainedLogicalCreateSlotBuilder logical();

  /**
   * <p>Create physical replication stream for process wal logs in binary form.</p>
   *
   * <p>Example usage:</p>
   * <pre>
   *   {@code
   *
   *    kwConnection
   *        .getReplicationAPI()
   *        .createReplicationSlot()
   *        .physical()
   *        .withSlotName("mySlot")
   *        .make();
   *
   *    KWReplicationStream stream =
   *        kwConnection
   *            .getReplicationAPI()
   *            .replicationStream()
   *            .physical()
   *            .withSlotName("mySlot")
   *            .start();
   *
   *    while (true) {
   *      ByteBuffer buffer = stream.read();
   *      //process binary WAL logs
   *    }
   *
   *   }
   * </pre>
   *
   * @return not null fluent api
   */
  ChainedPhysicalCreateSlotBuilder physical();
}
