package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

import java.util.function.BinaryOperator;

public abstract class Primitive2 extends AtomicExpression {

    public static class Wrapper extends Primitive2 {
        @NotNull private final BinaryOperator<Object> function;

        Wrapper(@NotNull BinaryOperator<Object> function, @NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
            super(argument1, argument2);
            this.function = function;
        }

        @Override
        public Object apply(Object arg1, Object arg2) {
            return function.apply(arg1, arg2);
        }
    }

    @NotNull private final AtomicExpression argument1;
    @NotNull private final AtomicExpression argument2;

    Primitive2(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    public AtomicExpression argument1() {
        return argument1;
    }

    public AtomicExpression argument2() {
        return argument2;
    }

    public abstract Object apply(Object arg1, Object arg2);

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive2(this);
    }
}
