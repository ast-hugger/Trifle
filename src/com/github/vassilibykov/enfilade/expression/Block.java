// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class Block extends ComplexExpression {
    public static Block with(List<? extends Expression> expressions) {
        return new Block(expressions);
    }

    @NotNull private final List<Expression> expressions;

    private Block(@NotNull List<? extends Expression> expressions) {
        this.expressions = Collections.unmodifiableList(expressions);
    }

    public List<Expression> expressions() {
        return expressions;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
