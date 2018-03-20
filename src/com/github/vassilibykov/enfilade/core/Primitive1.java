package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public abstract class Primitive1 extends AtomicExpression {
    @NotNull
    private final AtomicExpression argument;

    protected Primitive1(@NotNull AtomicExpression argument) {
        this.argument = argument;
    }

    public AtomicExpression argument() {
        return argument;
    }

    public abstract TypeCategory valueCategory();

    public abstract Object apply(Object arg);

    public abstract TypeCategory generate(GhostWriter writer, TypeCategory argCategory);

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive1(this);
    }
}
