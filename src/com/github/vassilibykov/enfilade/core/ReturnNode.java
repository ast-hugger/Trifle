// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * An explicit return (a bypass in the normal chain of continuations).
 */
class ReturnNode extends EvaluatorNode {
    @NotNull private final EvaluatorNode value;

    ReturnNode(@NotNull EvaluatorNode value) {
        this.value = value;
    }

    public EvaluatorNode value() {
        return value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitReturn(this);
    }

    @Override
    public String toString() {
        return "(ret " + value + ")";
    }
}
