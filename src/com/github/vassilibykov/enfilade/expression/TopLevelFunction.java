// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import com.github.vassilibykov.enfilade.core.Callable;

/**
 * Implemented by classes whose instances may appear as targets of
 * {@link FunctionReference}s.
 */
public interface TopLevelFunction {
    Callable asCallable();
    default int id() {
        return asCallable().id();
    }
}
