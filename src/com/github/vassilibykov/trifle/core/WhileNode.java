// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public class WhileNode extends EvaluatorNode {
    @NotNull private final EvaluatorNode condition;
    @NotNull private final EvaluatorNode body;
    /*internal*/ final AtomicLong bodyCount = new AtomicLong();

    public WhileNode(@NotNull EvaluatorNode condition, @NotNull EvaluatorNode body) {
        this.condition = condition;
        this.body = body;
    }

    public EvaluatorNode condition() {
        return condition;
    }

    public EvaluatorNode body() {
        return body;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitWhile(this);
    }

    @Override
    public String toString() {
        return "(while " + condition + " " + body + ")";
    }
}
