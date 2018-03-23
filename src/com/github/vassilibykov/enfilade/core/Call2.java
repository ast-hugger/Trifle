// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class Call2 extends CallExpression {
    @NotNull private final AtomicExpression arg1;
    @NotNull private final AtomicExpression arg2;

    Call2(Function function, @NotNull AtomicExpression arg1, @NotNull AtomicExpression arg2) {
        super(function);
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public AtomicExpression arg1() {
        return arg1;
    }

    public AtomicExpression arg2() {
        return arg2;
    }

    @Override
    protected int arity() {
        return 2;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall2(this);
    }
}
