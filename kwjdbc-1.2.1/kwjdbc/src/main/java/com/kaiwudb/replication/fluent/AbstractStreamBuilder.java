/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.replication.fluent;

import com.kaiwudb.replication.LogSequenceNumber;

import java.util.concurrent.TimeUnit;

public abstract class AbstractStreamBuilder<T extends ChainedCommonStreamBuilder<T>>
    implements ChainedCommonStreamBuilder<T> {
  private static final int DEFAULT_STATUS_INTERVAL = (int) TimeUnit.SECONDS.toMillis(10L);
  protected int statusIntervalMs = DEFAULT_STATUS_INTERVAL;
  protected LogSequenceNumber startPosition = LogSequenceNumber.INVALID_LSN;
  protected String slotName;

  protected abstract T self();

  @Override
  public T withStatusInterval(int time, TimeUnit format) {
    statusIntervalMs = (int) TimeUnit.MILLISECONDS.convert(time, format);
    return self();
  }

  @Override
  public T withStartPosition(LogSequenceNumber lsn) {
    this.startPosition = lsn;
    return self();
  }

  @Override
  public T withSlotName(String slotName) {
    this.slotName = slotName;
    return self();
  }
}
