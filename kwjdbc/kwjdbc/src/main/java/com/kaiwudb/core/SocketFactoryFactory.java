/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

import com.kaiwudb.ssl.LibPQFactory;
import com.kaiwudb.util.GT;
import com.kaiwudb.util.ObjectFactory;
import com.kaiwudb.KWProperty;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Instantiates {@link SocketFactory} based on the {@link KWProperty#SOCKET_FACTORY}.
 */
public class SocketFactoryFactory {

  /**
   * Instantiates {@link SocketFactory} based on the {@link KWProperty#SOCKET_FACTORY}.
   *
   * @param info connection properties
   * @return socket factory
   * @throws KSQLException if something goes wrong
   */
  public static SocketFactory getSocketFactory(Properties info) throws KSQLException {
    // Socket factory
    String socketFactoryClassName = KWProperty.SOCKET_FACTORY.get(info);
    if (socketFactoryClassName == null) {
      return SocketFactory.getDefault();
    }
    try {
      return (SocketFactory) ObjectFactory.instantiate(socketFactoryClassName, info, true,
          KWProperty.SOCKET_FACTORY_ARG.get(info));
    } catch (Exception e) {
      throw new KSQLException(
          GT.tr("The SocketFactory class provided {0} could not be instantiated.",
              socketFactoryClassName),
          KSQLState.CONNECTION_FAILURE, e);
    }
  }

  /**
   * Instantiates {@link SSLSocketFactory} based on the {@link KWProperty#SSL_FACTORY}.
   *
   * @param info connection properties
   * @return SSL socket factory
   * @throws KSQLException if something goes wrong
   */
  public static SSLSocketFactory getSslSocketFactory(Properties info) throws KSQLException {
    String classname = KWProperty.SSL_FACTORY.get(info);
    if (classname == null
      || "com.kaiwudb.ssl.jdbc4.LibPQFactory".equals(classname)
      || "com.kaiwudb.ssl.LibPQFactory".equals(classname)) {
      return new LibPQFactory(info);
    }
    try {
      return (SSLSocketFactory) ObjectFactory.instantiate(classname, info, true,
          KWProperty.SSL_FACTORY_ARG.get(info));
    } catch (Exception e) {
      throw new KSQLException(
          GT.tr("The SSLSocketFactory class provided {0} could not be instantiated.", classname),
          KSQLState.CONNECTION_FAILURE, e);
    }
  }

}
