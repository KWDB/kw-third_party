/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.xa;

import javax.transaction.xa.XAException;

/**
 * A convenience subclass of <code>XAException</code> which makes it easy to create an instance of
 * <code>XAException</code> with a human-readable message, a <code>Throwable</code> cause, and an XA
 * error code.
 *
 * @author Michael S. Allman
 */
public class KWXAException extends XAException {
  KWXAException(String message, int errorCode) {
    super(message);

    this.errorCode = errorCode;
  }

  KWXAException(String message, Throwable cause, int errorCode) {
    super(message);

    initCause(cause);
    this.errorCode = errorCode;
  }

  KWXAException(Throwable cause, int errorCode) {
    super(errorCode);

    initCause(cause);
  }
}
