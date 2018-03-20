package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class If extends ComplexExpression {
    @NotNull private final AtomicExpression condition;
    @NotNull private final Expression trueBranch;
    @NotNull private final Expression falseBranch;

    If(@NotNull AtomicExpression condition, @NotNull Expression trueBranch, @NotNull Expression falseBranch) {
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
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitIf(this);
    }

    @Override
    public String toString() {
        return "(if " + condition + " " + trueBranch + " " + falseBranch + ")";
    }
}
