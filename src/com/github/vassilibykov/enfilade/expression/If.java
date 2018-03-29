// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

public class If extends ComplexExpression {
    public static If with(AtomicExpression condition, Expression trueBranch, Expression falseBranch) {
        return new If(condition, trueBranch, falseBranch);
    }

    @NotNull private final AtomicExpression condition;
    @NotNull private final Expression trueBranch;
    @NotNull private final Expression falseBranch;

    private If(@NotNull AtomicExpression condition, @NotNull Expression trueBranch, @NotNull Expression falseBranch) {
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    public AtomicExpression condition() {
        return condition;
    }

    public Expression trueBranch() {
        return trueBranch;
    }

    public Expression falseBranch() {
        return falseBranch;
    }

    @Override
    public String toString() {
        return "if(" + condition + ") ...";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitIf(this);
    }
}
