/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.xa;

import com.kaiwudb.ds.common.KWObjectFactory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;

/**
 * An ObjectFactory implementation for KWXADataSource-objects.
 */

public class KWXADataSourceFactory extends KWObjectFactory {
  /*
   * All the other KaiwuDB DataSource use KWObjectFactory directly, but we can't do that with
   * KWXADataSource because referencing KWXADataSource from KWObjectFactory would break
   * "JDBC2 Enterprise" edition build which doesn't include KWXADataSource.
   */

  @Override
  public Object getObjectInstance(Object obj, Name name, Context nameCtx,
      Hashtable<?, ?> environment) throws Exception {
    Reference ref = (Reference) obj;
    String className = ref.getClassName();
    if (className.equals("KWXADataSource")) {
      return loadXADataSource(ref);
    } else {
      return null;
    }
  }

  private Object loadXADataSource(Reference ref) {
    KWXADataSource ds = new KWXADataSource();
    return loadBaseDataSource(ds, ref);
  }
}
