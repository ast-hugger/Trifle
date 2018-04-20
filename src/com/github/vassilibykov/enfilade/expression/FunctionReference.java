// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

/**
 * A reference to a top-level( built-in or user-defined) function. Can be used
 * as a {@link Call} target or appear as a standalone expression. In the latter
 * case it evaluates to a closure. When used as a call target, the system is
 * able to implement the call more efficiently than a regular closure call.
 */
public class FunctionReference extends AtomicExpression {
    public static FunctionReference to(TopLevelFunction callable) {
        return new FunctionReference(callable);
    }

    @NotNull private final TopLevelFunction target;

    private FunctionReference(@NotNull TopLevelFunction target) {
        this.target = target;
    }

    public TopLevelFunction target() {
        return target;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFunctionReference(this);
    }
}
