/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.hostchooser;

import java.util.Iterator;

/**
 * Lists connections in preferred order.
 */
public interface HostChooser extends Iterable<CandidateHost> {
  /**
   * Lists connection hosts in preferred order.
   *
   * @return connection hosts in preferred order.
   */
  @Override
  Iterator<CandidateHost> iterator();
}
