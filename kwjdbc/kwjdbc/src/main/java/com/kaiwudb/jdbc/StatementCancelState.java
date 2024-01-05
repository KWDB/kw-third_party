/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.jdbc;

/**
 * Represents {@link KwStatement#cancel()} state.
 */
enum StatementCancelState {
    IDLE,
    IN_QUERY,
    CANCELING,
    CANCELLED
}
