// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

import org.jetbrains.annotations.NotNull;

public class SetVariable extends ComplexExpression {
    public static SetVariable with(Variable variable, Expression value) {
        return new SetVariable(variable, value);
    }

    @NotNull private final Variable variable;
    @NotNull private final Expression value;

    private SetVariable(@NotNull Variable variable, @NotNull Expression value) {
        this.variable = variable;
        this.value = value;
    }

    public Variable variable() {
        return variable;
    }

    public Expression value() {
        return value;
    }

    @Override
    public String toString() {
        return "set(" + variable + ", " + value + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSetVariable(this);
    }
}
