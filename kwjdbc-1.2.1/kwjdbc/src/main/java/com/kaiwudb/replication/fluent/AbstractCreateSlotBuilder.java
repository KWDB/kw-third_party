/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent;

import com.kaiwudb.core.BaseConnection;
import com.kaiwudb.core.ServerVersion;
import com.kaiwudb.util.GT;

import java.sql.SQLFeatureNotSupportedException;

public abstract class AbstractCreateSlotBuilder<T extends ChainedCommonCreateSlotBuilder<T>>
    implements ChainedCommonCreateSlotBuilder<T> {

  protected String slotName;
  protected boolean temporaryOption = false;
  protected BaseConnection connection;

  protected AbstractCreateSlotBuilder(BaseConnection connection) {
    this.connection = connection;
  }

  protected abstract T self();

  @Override
  public T withSlotName(String slotName) {
    this.slotName = slotName;
    return self();
  }

  @Override
  public T withTemporaryOption() throws SQLFeatureNotSupportedException {

    if (!connection.haveMinimumServerVersion(ServerVersion.v10)) {
      throw new SQLFeatureNotSupportedException(
          GT.tr("Server does not support temporary replication slots")
      );
    }

    this.temporaryOption = true;
    return self();
  }
}
