// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

public class Return extends ComplexExpression {
    public static Return with(AtomicExpression value) {
        return new Return(value);
    }

    @NotNull private final AtomicExpression value;

    private Return(@NotNull AtomicExpression value) {
        this.value = value;
    }

    public AtomicExpression value() {
        return value;
    }

    @Override
    public String toString() {
        return "return(" + value + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitReturn(this);
    }
}
