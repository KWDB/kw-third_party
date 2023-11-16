/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

import com.kaiwudb.util.GT;
import com.kaiwudb.KWProperty;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;

import java.util.Properties;

public enum SslMode {
  /**
   * Do not use encrypted connections.
   */
  DISABLE("disable"),
  /**
   * Start with non-encrypted connection, then try encrypted one.
   */
  ALLOW("allow"),
  /**
   * Start with encrypted connection, fallback to non-encrypted (default).
   */
  PREFER("prefer"),
  /**
   * Ensure connection is encrypted.
   */
  REQUIRE("require"),
  /**
   * Ensure connection is encrypted, and client trusts server certificate.
   */
  VERIFY_CA("verify-ca"),
  /**
   * Ensure connection is encrypted, client trusts server certificate, and server hostname matches
   * the one listed in the server certificate.
   */
  VERIFY_FULL("verify-full"),
  ;

  public static final SslMode[] VALUES = values();

  public final String value;

  SslMode(String value) {
    this.value = value;
  }

  public boolean requireEncryption() {
    return this.compareTo(REQUIRE) >= 0;
  }

  public boolean verifyCertificate() {
    return this == VERIFY_CA || this == VERIFY_FULL;
  }

  public boolean verifyPeerName() {
    return this == VERIFY_FULL;
  }

  public static SslMode of(Properties info) throws KSQLException {
    String sslmode = KWProperty.SSL_MODE.get(info);
    // If sslmode is not set, fallback to ssl parameter
    if (sslmode == null) {
      if (KWProperty.SSL.getBoolean(info) || "".equals(KWProperty.SSL.get(info))) {
        return VERIFY_FULL;
      }
      return PREFER;
    }

    for (SslMode sslMode : VALUES) {
      if (sslMode.value.equalsIgnoreCase(sslmode)) {
        return sslMode;
      }
    }
    throw new KSQLException(GT.tr("Invalid sslmode value: {0}", sslmode),
        KSQLState.CONNECTION_UNABLE_TO_CONNECT);
  }
}
