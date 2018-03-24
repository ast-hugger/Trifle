// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * A sequence, like {@code begin} in Scheme or {@code progn} in Common Lisp. In
 * theory it's expressible as a chain of {@code let}s (in our variant, not in
 * classical A-normal forms), but it's convenient to treat it as a distinct
 * construct.
 */
public class Block extends ComplexExpression {
    @NotNull private final Expression[] expressions;

    Block(@NotNull Expression[] expressions) {
        this.expressions = expressions;
    }

    public Expression[] expressions() {
        return expressions;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBlock(this);
    }

    @Override
    public String toString() {
        return "(prog [" + expressions.length + "])";
    }
}
