// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class Ret extends AtomicExpression {
    // TODO is it atomic or complex? theoretically it's probably complex but in practice we might want to consider it atomic

    @NotNull private final AtomicExpression value;

    Ret(@NotNull AtomicExpression value) {
        this.value = value;
    }

    public AtomicExpression value() {
        return value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitRet(this);
    }

    @Override
    public String toString() {
        return "(ret " + value + ")";
    }
}
