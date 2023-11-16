/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

import java.io.IOException;

public class KWBindException extends IOException {

  private final IOException ioe;

  public KWBindException(IOException ioe) {
    this.ioe = ioe;
  }

  public IOException getIOException() {
    return ioe;
  }
}
