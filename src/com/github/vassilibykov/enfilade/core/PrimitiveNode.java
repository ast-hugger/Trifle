// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Primitive;
import org.jetbrains.annotations.NotNull;

public abstract class PrimitiveNode extends EvaluatorNode {
    @NotNull protected final Primitive implementation;

    public PrimitiveNode(@NotNull Primitive implementation) {
        this.implementation = implementation;
    }

    public Primitive implementation() {
        return implementation;
    }
}
