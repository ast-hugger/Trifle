// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class Call1 extends Call {
    @NotNull private final AtomicExpression arg;

    Call1(Function function, @NotNull AtomicExpression arg) {
        super(function);
        this.arg = arg;
    }

    public AtomicExpression arg() {
        return arg;
    }

    @Override
    protected int arity() {
        return 1;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall1(this);
    }
}
