/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

/**
 * Represents a provider of results.
 *
 * @param <T> the type of results provided by this provider
 */
public interface Provider<T> {

  /**
   * Gets a result.
   *
   * @return a result
   */
  T get();
}
