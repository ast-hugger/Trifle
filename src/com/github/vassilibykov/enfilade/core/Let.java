// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * The classic {@code let} form, introducing a variable and binding its value.
 */
public class Let extends ComplexExpression {
    @NotNull private final Var variable;
    @NotNull private final Expression initializer;
    @NotNull private final Expression body;

    Let(@NotNull Var variable, @NotNull Expression initializer, @NotNull Expression body) {
        this.variable = variable;
        this.initializer = initializer;
        this.body = body;
    }

    public Var variable() {
        return variable;
    }

    public Expression initializer() {
        return initializer;
    }

    public Expression body() {
        return body;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLet(this);
    }

    @Override
    public String toString() {
        return "(let (" + variable + " " + initializer + ") " + body + ")";
    }
}
