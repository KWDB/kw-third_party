/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent.logical;

import com.kaiwudb.replication.fluent.CommonOptions;

import java.util.Properties;

public interface LogicalReplicationOptions extends CommonOptions {
  /**
   * Required parameter for logical replication.
   *
   * @return not null logical replication slot name that already exists on server and free.
   */
  @Override
  String getSlotName();

  /**
   * Parameters for output plugin. Parameters will be set to output plugin that register for
   * specified replication slot name.
   *
   * @return list options that will be pass to output_plugin for that was create replication slot
   */
  Properties getSlotOptions();
}
