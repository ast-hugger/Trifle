// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MethodDeclaration extends Block {

    @NotNull private final String selector;

    MethodDeclaration(
        @NotNull String selector,
        @NotNull List<String> argumentNames,
        @NotNull List<String> tempNames,
        @NotNull List<AstNode> expressions)
    {
        super(argumentNames, tempNames, expressions);
        this.selector = selector;
    }

    public String selector() {
        return selector;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitMethodDeclaration(this);
    }
}
