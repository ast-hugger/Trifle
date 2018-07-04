// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class Block extends AstNode {

    @NotNull private final List<String> argumentNames;
    @NotNull private final List<String> tempNames;
    @NotNull private final List<AstNode> expressions;

    Block(@NotNull List<String> argumentNames, @NotNull List<String> tempNames, @NotNull List<AstNode> expressions) {
        this.argumentNames = Collections.unmodifiableList(argumentNames);
        this.tempNames = Collections.unmodifiableList(tempNames);
        this.expressions = Collections.unmodifiableList(expressions);
    }

    public List<String> argumentNames() {
        return argumentNames;
    }

    public List<String> tempNames() {
        return tempNames;
    }

    public List<AstNode> expressions() {
        return expressions;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
