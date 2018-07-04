// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

public class Return extends AstNode {

    @NotNull private final AstNode expression;

    Return(@NotNull AstNode expression) {
        this.expression = expression;
    }

    public AstNode expression() {
        return expression;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitReturn(this);
    }
}
