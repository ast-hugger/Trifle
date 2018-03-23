// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class Call0 extends CallExpression {

    Call0(Function function) {
        super(function);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall0(this);
    }

    @Override
    protected int arity() {
        return 0;
    }
}
