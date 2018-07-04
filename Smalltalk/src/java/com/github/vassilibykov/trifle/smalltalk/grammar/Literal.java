// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

public class Literal extends AstNode {

    private final Object value;

    Literal(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLiteral(this);
    }
}
