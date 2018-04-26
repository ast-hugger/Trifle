// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.primitive.Primitive1;
import org.jetbrains.annotations.NotNull;

/**
 * A call of a unary primitive. This is an abstract class with a variety of
 * implementations in the {@link com.github.vassilibykov.trifle.primitive} package.
 * Note that we conflate the notions of a primitive and a call of a primitive.
 */
public class Primitive1Node extends PrimitiveNode {
    @NotNull private final EvaluatorNode argument;

    protected Primitive1Node(@NotNull Primitive1 primitive, @NotNull EvaluatorNode argument) {
        super(primitive);
        this.argument = argument;
    }

    public Primitive1 implementation() {
        return (Primitive1) implementation;
    }

    public EvaluatorNode argument() {
        return argument;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive1(this);
    }
}
