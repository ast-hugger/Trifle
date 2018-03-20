package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public abstract class Primitive1 extends AtomicExpression {

    public static class Wrapper extends Primitive1 {
        @NotNull private final UnaryOperator<Object> function;

        public Wrapper(@NotNull UnaryOperator<Object> function, @NotNull AtomicExpression argument) {
            super(argument);
            this.function = function;
        }

        @Override
        public TypeCategory valueCategory() {
            return TypeCategory.REFERENCE;
        }

        @Override
        public void generate(GhostWriter writer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object apply(Object arg) {
            return function.apply(arg);
        }
    }

    @NotNull private final AtomicExpression argument;

    protected Primitive1(@NotNull AtomicExpression argument) {
        this.argument = argument;
    }

    public AtomicExpression argument() {
        return argument;
    }

    public abstract TypeCategory valueCategory();

    public abstract Object apply(Object arg);

    public abstract void generate(GhostWriter writer);

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive1(this);
    }
}
