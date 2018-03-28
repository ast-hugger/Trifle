// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The conditional.
 */
class IfNode extends EvaluatorNode {
    @NotNull private final EvaluatorNode condition;
    @NotNull private final EvaluatorNode trueBranch;
    @NotNull private final EvaluatorNode falseBranch;
    /*internal*/ final AtomicLong trueBranchCount = new AtomicLong();
    /*internal*/ final AtomicLong falseBranchCount = new AtomicLong();

    IfNode(@NotNull EvaluatorNode condition, @NotNull EvaluatorNode trueBranch, @NotNull EvaluatorNode falseBranch) {
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    public EvaluatorNode condition() {
        return condition;
    }

    public EvaluatorNode trueBranch() {
        return trueBranch;
    }

    public EvaluatorNode falseBranch() {
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
