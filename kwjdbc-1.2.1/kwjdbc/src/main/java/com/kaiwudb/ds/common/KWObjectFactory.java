/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.ds.common;

import com.kaiwudb.ds.KWConnectionPoolDataSource;
import com.kaiwudb.ds.KWPoolingDataSource;
import com.kaiwudb.ds.KWSimpleDataSource;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * Returns a DataSource-ish thing based on a JNDI reference. In the case of a SimpleDataSource or
 * ConnectionPool, a new instance is created each time, as there is no connection state to maintain.
 * In the case of a PoolingDataSource, the same DataSource will be returned for every invocation
 * within the same VM/ClassLoader, so that the state of the connections in the pool will be
 * consistent.
 *
 * @author Aaron Mulder (ammulder@chariotsolutions.com)
 */
public class KWObjectFactory implements ObjectFactory {
  /**
   * Dereferences a KaiwuDB DataSource. Other types of references are ignored.
   */
  @Override
  public Object getObjectInstance(Object obj, Name name, Context nameCtx,
      Hashtable<?, ?> environment) throws Exception {
    Reference ref = (Reference) obj;
    String className = ref.getClassName();
    // Old names are here for those who still use them
    if (className.equals("KWSimpleDataSource")
        || className.equals("SimpleDataSource")
        || className.equals("Jdbc3SimpleDataSource")) {
      return loadSimpleDataSource(ref);
    } else if (className.equals("KWConnectionPoolDataSource")
        || className.equals("ConnectionPool")
        || className.equals("Jdbc3ConnectionPool")) {
      return loadConnectionPool(ref);
    } else if (className.equals("KWPoolingDataSource")
        || className.equals("PoolingDataSource")
        || className.equals("Jdbc3PoolingDataSource")) {
      return loadPoolingDataSource(ref);
    } else {
      return null;
    }
  }

  private Object loadPoolingDataSource(Reference ref) {
    // If DataSource exists, return it
    String name = getProperty(ref, "dataSourceName");
    KWPoolingDataSource pds = KWPoolingDataSource.getDataSource(name);
    if (pds != null) {
      return pds;
    }
    // Otherwise, create a new one
    pds = new KWPoolingDataSource();
    pds.setDataSourceName(name);
    loadBaseDataSource(pds, ref);
    String min = getProperty(ref, "initialConnections");
    if (min != null) {
      pds.setInitialConnections(Integer.parseInt(min));
    }
    String max = getProperty(ref, "maxConnections");
    if (max != null) {
      pds.setMaxConnections(Integer.parseInt(max));
    }
    return pds;
  }

  private Object loadSimpleDataSource(Reference ref) {
    KWSimpleDataSource ds = new KWSimpleDataSource();
    return loadBaseDataSource(ds, ref);
  }

  private Object loadConnectionPool(Reference ref) {
    KWConnectionPoolDataSource cp = new KWConnectionPoolDataSource();
    return loadBaseDataSource(cp, ref);
  }

  protected Object loadBaseDataSource(BaseDataSource ds, Reference ref) {
    ds.setFromReference(ref);

    return ds;
  }

  protected String getProperty(Reference ref, String s) {
    RefAddr addr = ref.get(s);
    if (addr == null) {
      return null;
    }
    return (String) addr.getContent();
  }

}
