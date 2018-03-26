// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * A function call.
 */
public class Call extends ComplexExpression {
    public static Call with(Function target, AtomicExpression... arguments) {
        return new Call(target, List.of(arguments));
    }

    public static Call with(Function target, List<AtomicExpression> arguments) {
        return new Call(target, arguments);
    }

    @NotNull private final Function target;
    @NotNull private final List<AtomicExpression> arguments;

    private Call(@NotNull Function target, @NotNull List<AtomicExpression> arguments) {
        this.target = target;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    public Function target() {
        return target;
    }

    public List<AtomicExpression> arguments() {
        return arguments;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall(this);
    }
}