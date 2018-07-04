// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

public class VarReference extends AstNode {

    @NotNull private final String name;

    VarReference(@NotNull String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVarReference(this);
    }
}
