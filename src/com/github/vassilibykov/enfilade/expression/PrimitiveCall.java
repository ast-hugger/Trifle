// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PrimitiveCall extends AtomicExpression {
    public static PrimitiveCall with(Class<? extends Primitive> target, AtomicExpression... arguments) {
        return new PrimitiveCall(target, List.of(arguments));
    }

    public static PrimitiveCall with(Class<? extends Primitive> target, List<? extends AtomicExpression> arguments) {
        return new PrimitiveCall(target, arguments);
    }

    @NotNull private final Class<? extends Primitive> target;
    @NotNull private final List<AtomicExpression> arguments;

    private PrimitiveCall(@NotNull Class<? extends Primitive> target, @NotNull List<? extends AtomicExpression> arguments) {
        this.target = target;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    public Class<? extends Primitive> target() {
        return target;
    }

    public List<AtomicExpression> arguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return target.getSimpleName()
            + "("
            + arguments.stream().map(it -> it.toString()).collect(Collectors.joining(", "))
            + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitiveCall(this);
    }
}
