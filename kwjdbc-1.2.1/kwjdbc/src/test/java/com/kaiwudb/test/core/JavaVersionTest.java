/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.core;

import com.kaiwudb.core.JavaVersion;

import org.junit.Assert;
import org.junit.Test;

public class JavaVersionTest {
  @Test
  public void testGetRuntimeVersion() {
    String currentVersion = System.getProperty("java.version");
    String msg = "java.version = " + currentVersion + ", JavaVersion.getRuntimeVersion() = "
        + JavaVersion.getRuntimeVersion();
    System.out.println(msg);
    if (currentVersion.startsWith("1.8")) {
      Assert.assertEquals(msg, JavaVersion.v1_8, JavaVersion.getRuntimeVersion());
    }
  }
}
