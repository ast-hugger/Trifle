// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

/**
 * A reference to a built-in or top-level function.
 */
public class FunctionReference extends AtomicExpression {
    public static FunctionReference to(DirectlyCallable callable) {
        return new FunctionReference(callable);
    }

    @NotNull private final DirectlyCallable target;

    private FunctionReference(@NotNull DirectlyCallable target) {
        this.target = target;
    }

    public DirectlyCallable target() {
        return target;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFunctionReference(this);
    }
}
