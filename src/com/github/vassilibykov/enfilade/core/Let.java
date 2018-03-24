// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * The classic {@code let} form, introducing a variable and binding its value.
 *
 * <p>Note that the {@link #initializer} expression is treated differently from
 * pure A-normal forms. There, {@code let} is defined as a sibling of complex
 * and atomic expression types, so it is neither. The initializer of a let-bound
 * variable is typed as a complex expression. Together, these mean that a
 * let-bound variable initializer cannot be an atomic expression or a let form.
 *
 * <p>Our interest in A-normal forms is pragmatic, mainly for the stack usage
 * discipline they impose when used as the IR of a dynamic language compiler
 * front end. For that purpose, these classic restrictions are unnecessary, so
 * we don't impose them (at least not until we discover that they are).
 */
public class Let extends ComplexExpression {
    @NotNull private final Variable variable;
    @NotNull private final Expression initializer;
    @NotNull private final Expression body;

    Let(@NotNull Variable variable, @NotNull Expression initializer, @NotNull Expression body) {
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

    @Override
    public String toString() {
        return "(let (" + variable + " " + initializer + ") " + body + ")";
    }
}
