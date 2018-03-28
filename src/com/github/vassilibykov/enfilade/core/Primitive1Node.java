// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * A call of a unary primitive. This is an abstract class with a variety of
 * implementations in the {@link com.github.vassilibykov.enfilade.primitives} package.
 * Note that we conflate the notions of a primitive and a call of a primitive.
 */
public abstract class Primitive1Node extends EvaluatorNode {
    @NotNull
    private final EvaluatorNode argument;

    protected Primitive1Node(@NotNull EvaluatorNode argument) {
        this.argument = argument;
    }

    public EvaluatorNode argument() {
        return argument;
    }

    public abstract JvmType jvmType();

    public abstract Object apply(Object arg);

    public abstract JvmType generate(GhostWriter writer, JvmType argCategory);

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive1(this);
    }
}
