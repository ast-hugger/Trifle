// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import com.github.vassilibykov.enfilade.Callable;
import com.github.vassilibykov.enfilade.core.FunctionImplementation;

/**
 * A user-defined function created and managed by {@link TopLevel}.
 */
public class TopLevelFunction implements DirectlyCallable {
    private FunctionImplementation implementation;

    @Override
    public Callable asCallable() {
        return implementation;
    }

    FunctionImplementation implementation() {
        return implementation;
    }

    void setImplementation(FunctionImplementation implementation) {
        this.implementation = implementation;
    }

}
