// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.primitives.Primitive2;
import org.jetbrains.annotations.NotNull;

/**
 * A call of a binary primitive. This is an abstract class with a variety of
 * implementations in the {@link com.github.vassilibykov.enfilade.primitives} package.
 * Note that we conflate the notions of a primitive and a call of a primitive.
 */
public class Primitive2Node extends PrimitiveNode {
    @NotNull private final EvaluatorNode argument1;
    @NotNull private final EvaluatorNode argument2;

    protected Primitive2Node(@NotNull Primitive2 primitive, @NotNull EvaluatorNode argument1, @NotNull EvaluatorNode argument2) {
        super(primitive);
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    public Primitive2 implementation() {
        return (Primitive2) implementation;
    }

    public EvaluatorNode argument1() {
        return argument1;
    }

    public EvaluatorNode argument2() {
        return argument2;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive2(this);
    }
}
