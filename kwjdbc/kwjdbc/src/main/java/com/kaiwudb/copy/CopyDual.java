/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.copy;

/**
 * Bidirectional via copy stream protocol. Via bidirectional copy protocol work KaiwuDB
 * replication.
 *
 * @see CopyIn
 * @see CopyOut
 */
public interface CopyDual extends CopyIn, CopyOut {
}
