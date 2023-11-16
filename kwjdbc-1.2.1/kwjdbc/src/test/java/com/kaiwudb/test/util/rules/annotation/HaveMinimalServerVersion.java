/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.test.util.rules.annotation;

import com.kaiwudb.core.ServerVersion;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation use to ignore test if the current server version less than specified version.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {METHOD, TYPE})
public @interface HaveMinimalServerVersion {
  /**
   * @return not null sever version in form x.y.z like 9.4, 9.5.3, etc.
   * @see ServerVersion
   */
  String value();
}
