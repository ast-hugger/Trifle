// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class LetrecNode extends LetNode {
    LetrecNode(@NotNull VariableDefinition variable, @NotNull EvaluatorNode initializer, @NotNull EvaluatorNode body) {
        super(variable, initializer, body);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLetrec(this);
    }

    @Override
    public String toString() {
        return "(letrec (" + variable() + " ...) ...)";
    }
}
