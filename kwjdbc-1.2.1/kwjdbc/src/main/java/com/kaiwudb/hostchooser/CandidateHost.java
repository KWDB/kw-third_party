/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.hostchooser;

import com.kaiwudb.util.HostSpec;

/**
 * Candidate host to be connected.
 */
public class CandidateHost {
  public final HostSpec hostSpec;
  public final HostRequirement targetServerType;

  public CandidateHost(HostSpec hostSpec, HostRequirement targetServerType) {
    this.hostSpec = hostSpec;
    this.targetServerType = targetServerType;
  }
}
