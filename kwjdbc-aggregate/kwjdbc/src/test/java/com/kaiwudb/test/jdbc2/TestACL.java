/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.jdbc2;

import com.kaiwudb.jdbc.KwConnection;
import com.kaiwudb.jdbc.KwDatabaseMetaData;

import org.junit.Test;

public class TestACL {

  @Test
  public void testParseACL() {
    KwConnection kwConnection = null;
    KwDatabaseMetaData a = new KwDatabaseMetaData(kwConnection) {
    };
    a.parseACL("{jurka=arwdRxt/jurka,permuser=rw*/jurka}", "jurka");
    a.parseACL("{jurka=a*r*w*d*R*x*t*/jurka,permuser=rw*/jurka}", "jurka");
    a.parseACL("{=,jurka=arwdRxt,permuser=rw}", "jurka");
    a.parseACL("{jurka=arwdRxt/jurka,permuser=rw*/jurka,grantuser=w/permuser}", "jurka");
    a.parseACL("{jurka=a*r*w*d*R*x*t*/jurka,permuser=rw*/jurka,grantuser=w/permuser}", "jurka");
    a.parseACL(
        "{jurka=arwdRxt/jurka,permuser=rw*/jurka,grantuser=w/permuser,\"group permgroup=a/jurka\"}",
        "jurka");
    a.parseACL(
        "{jurka=a*r*w*d*R*x*t*/jurka,permuser=rw*/jurka,grantuser=w/permuser,\"group permgroup=a/jurka\"}",
        "jurka");
  }
}
