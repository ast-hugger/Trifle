// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

/**
 * The {@code let} form. Note that the initializer is allowed to be an arbitrary
 * expression. In canonical ANF the initializer must be a complex expression.
 */
public class Let extends ComplexExpression {
    public static Let with(Variable variable, Expression initializer, Expression body) {
        return new Let(variable, initializer, body);
    }

    @NotNull private final Variable variable;
    @NotNull private final Expression initializer;
    @NotNull private final Expression body;

    private Let(@NotNull Variable variable, @NotNull Expression initializer, @NotNull Expression body) {
        this.variable = variable;
        this.initializer = initializer;
        this.body = body;
    }

    public Variable variable() {
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
}
