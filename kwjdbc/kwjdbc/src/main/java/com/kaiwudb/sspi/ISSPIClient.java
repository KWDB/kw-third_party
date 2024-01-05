/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.sspi;

import java.io.IOException;
import java.sql.SQLException;

/**
 * <p>Use Waffle-JNI to support SSPI authentication when KwJDBC is running on a Windows
 * client and talking to a Windows server.</p>
 *
 * <p>SSPI is not supported on a non-Windows client.</p>
 */
public interface ISSPIClient {
  boolean isSSPISupported();

  void startSSPI() throws SQLException, IOException;

  void continueSSPI(int msgLength) throws SQLException, IOException;

  void dispose();
}
