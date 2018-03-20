// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public abstract class Primitive2 extends AtomicExpression {
    @NotNull private final AtomicExpression argument1;
    @NotNull private final AtomicExpression argument2;

    protected Primitive2(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    public AtomicExpression argument1() {
        return argument1;
    }

    public AtomicExpression argument2() {
        return argument2;
    }

    public abstract TypeCategory valueCategory();

    public abstract Object apply(Object arg1, Object arg2);

    public abstract TypeCategory generate(GhostWriter writer, TypeCategory arg1Category, TypeCategory arg2Category);

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive2(this);
    }
}
