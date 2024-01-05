/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.hostchooser;

/**
 * Known state of a server.
 */
public enum HostStatus {
  ConnectFail,
  ConnectOK,
  Master,
  Secondary
}
