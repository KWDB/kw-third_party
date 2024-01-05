/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.util;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created by davec on 3/14/17.
 */
public class NullOutputStream extends PrintStream {

  public NullOutputStream(OutputStream out) {
    super(out);
  }

  @Override
  public void write(int b) {

  }

  @Override
  public void write(byte[] buf, int off, int len) {

  }
}
