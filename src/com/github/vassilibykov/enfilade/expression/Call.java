// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * A function call.
 */
public class Call extends ComplexExpression {
    public static Call with(Callable target, AtomicExpression... arguments) {
        return new Call(target, List.of(arguments));
    }

    public static Call with(Callable target, List<AtomicExpression> arguments) {
        return new Call(target, arguments);
    }

    @NotNull private final Callable target;
    @NotNull private final List<AtomicExpression> arguments;

    private Call(@NotNull Callable target, @NotNull List<AtomicExpression> arguments) {
        this.target = target;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    public Callable target() {
        return target;
    }

    public List<AtomicExpression> arguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "call(" + target + ", [" + arguments.size() + " args])";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall(this);
    }
}
