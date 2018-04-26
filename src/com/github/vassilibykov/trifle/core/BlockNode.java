// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.jetbrains.annotations.NotNull;

/**
 * An executable representation of a {@link com.github.vassilibykov.trifle.expression.Block}
 * expression.
 */
class BlockNode extends EvaluatorNode {
    @NotNull private final EvaluatorNode[] expressions;

    BlockNode(@NotNull EvaluatorNode[] expressions) {
        this.expressions = expressions;
    }

    public EvaluatorNode[] expressions() {
        return expressions;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBlock(this);
    }

    @Override
    public String toString() {
        return "(block [" + expressions.length + "])";
    }
}
