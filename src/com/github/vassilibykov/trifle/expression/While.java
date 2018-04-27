// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

import org.jetbrains.annotations.NotNull;

public class While extends ComplexExpression {
    public static While with(AtomicExpression condition, Expression body) {
        return new While(condition, body);
    }

    @NotNull private final AtomicExpression condition;
    @NotNull private final Expression body;

    private While(@NotNull AtomicExpression condition, @NotNull Expression body) {
        this.condition = condition;
        this.body = body;
    }

    public Expression condition() {
        return condition;
    }

    public Expression body() {
        return body;
    }

    @Override
    public String toString() {
        return "while (" + condition + ") " + body;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitWhile(this);
    }
}
