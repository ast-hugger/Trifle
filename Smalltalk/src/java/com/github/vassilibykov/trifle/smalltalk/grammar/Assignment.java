// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

public class Assignment extends AstNode {

    @NotNull private final String variableName;
    @NotNull private final AstNode expression;

    Assignment(@NotNull String variableName, @NotNull AstNode expression) {
        this.variableName = variableName;
        this.expression = expression;
    }

    public String variableName() {
        return variableName;
    }

    public AstNode expression() {
        return expression;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitAssignment(this);
    }
}
