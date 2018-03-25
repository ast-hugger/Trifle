// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PrimitiveCall extends AtomicExpression {
    public static PrimitiveCall with(PrimitiveCallTarget target, AtomicExpression... arguments) {
        return new PrimitiveCall(target, List.of(arguments));
    }

    public static PrimitiveCall with(PrimitiveCallTarget target, List<? extends AtomicExpression> arguments) {
        return new PrimitiveCall(target, arguments);
    }

    @NotNull private final PrimitiveCallTarget target;
    @NotNull private final List<AtomicExpression> arguments;

    private PrimitiveCall(@NotNull PrimitiveCallTarget target, @NotNull List<? extends AtomicExpression> arguments) {
        this.target = target;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive();
    }
}
