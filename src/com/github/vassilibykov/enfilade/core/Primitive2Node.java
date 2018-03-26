// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * A call of a binary primitive.
 */
public abstract class Primitive2Node extends EvaluatorNode {
    @NotNull private final EvaluatorNode argument1;
    @NotNull private final EvaluatorNode argument2;

    protected Primitive2Node(@NotNull EvaluatorNode argument1, @NotNull EvaluatorNode argument2) {
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    public EvaluatorNode argument1() {
        return argument1;
    }

    public EvaluatorNode argument2() {
        return argument2;
    }

    public abstract JvmType valueCategory();

    public abstract Object apply(Object arg1, Object arg2);

    public abstract JvmType generate(GhostWriter writer, JvmType arg1Category, JvmType arg2Category);

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive2(this);
    }
}
