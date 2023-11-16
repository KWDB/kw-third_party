/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent.logical;

import com.kaiwudb.replication.fluent.ChainedCommonCreateSlotBuilder;

/**
 * Logical replication slot specific parameters.
 */
public interface ChainedLogicalCreateSlotBuilder
    extends ChainedCommonCreateSlotBuilder<ChainedLogicalCreateSlotBuilder> {

  /**
   * <p>Output plugin that should be use for decode physical represent WAL to some logical form.
   * Output plugin should be installed on server(exists in shared_preload_libraries).</p>
   *
   * <p>Package kaiwudb-contrib provides sample output plugin <b>test_decoding</b> that can be
   * use for test logical replication api</p>
   *
   * @param outputPlugin not null name of the output plugin used for logical decoding
   * @return the logical slot builder
   */
  ChainedLogicalCreateSlotBuilder withOutputPlugin(String outputPlugin);
}
