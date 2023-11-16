/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

import com.kaiwudb.KWNotification;

public class Notification implements KWNotification {

  private final String name;
  private final String parameter;
  private final int pid;

  public Notification(String name, int pid) {
    this(name, pid, "");
  }

  public Notification(String name, int pid, String parameter) {
    this.name = name;
    this.pid = pid;
    this.parameter = parameter;
  }

  /*
   * Returns name of this notification
   */
  @Override
  public String getName() {
    return name;
  }

  /*
   * Returns the process id of the backend process making this notification
   */
  @Override
  public int getPID() {
    return pid;
  }

  @Override
  public String getParameter() {
    return parameter;
  }

}

