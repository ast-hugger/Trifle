// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

public class Const extends AtomicExpression {
    public static Const value(Object value) {
        return new Const(value);
    }

    private final Object value;

    private Const(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }

    @Override
    public String toString() {
        return "const(" + value + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitConst(this);
    }
}
