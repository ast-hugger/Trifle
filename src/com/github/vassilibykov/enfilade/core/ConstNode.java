// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.Nullable;

public class ConstNode extends EvaluatorNode {
    @Nullable private final Object value;

    public ConstNode(@Nullable Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitConst(this);
    }

    @Override
    public String toString() {
        return "(const " + value + ")";
    }
}
