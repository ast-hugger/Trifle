// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * A function value in a program. Produced by evaluating a lambda expression or
 * by referring to a top-level function.
 */
public class Closure {
    @NotNull /*internal*/ final FunctionImplementation implementation;
    @NotNull /*internal*/ final Object[][] outerFrames;
    @NotNull /*internal*/ final Object[] copiedValues;

    Closure(@NotNull FunctionImplementation implementation, @NotNull Object[] copiedValues) {
        this.implementation = implementation;
        this.outerFrames = new Object[0][];
        this.copiedValues = copiedValues;
    }

    public Object invoke() {
        return implementation.execute(this);
    }

    public Object invoke(Object arg) {
        return implementation.execute(this, arg);
    }

    public Object invoke(Object arg1, Object arg2) {
        return implementation.execute(this, arg1, arg2);
    }
}
