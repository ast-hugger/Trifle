// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.core.FreeFunction;
import com.github.vassilibykov.trifle.core.FreeFunctionCallDispatcher;
import org.jetbrains.annotations.NotNull;

/**
 * A reference to a top-level (built-in or user-defined) function. Can be used
 * as a {@link Call} target or appear as a standalone expression. In the latter
 * case it evaluates to a closure. When used as a call target, which is the
 * chief intended usage, the system is able to implement the call more
 * efficiently than a regular closure call.
 */
public class FreeFunctionReference extends AtomicExpression {
    public static FreeFunctionReference to(FreeFunction target) {
        return new FreeFunctionReference(target);
    }

    @NotNull private final FreeFunction target;

    private FreeFunctionReference(@NotNull FreeFunction target) {
        this.target = target;
    }

    public FreeFunction target() {
        return target;
    }

    @Override
    public CallDispatcher createDispatcher(Visitor<EvaluatorNode> translator) {
        return new FreeFunctionCallDispatcher(target);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFunctionReference(this);
    }
}
