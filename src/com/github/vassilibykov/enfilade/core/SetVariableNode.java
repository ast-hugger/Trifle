// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * An expression mutating a variable to have a new value. The new value, which
 * is also the value of this expression, must be an atomic expression.
 */
public class SetVariableNode extends EvaluatorNode {
    @NotNull /*internal*/ final VariableDefinition variable;
    @NotNull private final EvaluatorNode value;

    SetVariableNode(@NotNull VariableDefinition variable, @NotNull EvaluatorNode value) {
        this.variable = variable;
        this.value = value;
    }

    public VariableDefinition variable() {
        return variable;
    }

    public EvaluatorNode value() {
        return value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVarSet(this);
    }

    @Override
    public String toString() {
        return "(set! " + variable + " " + value + ")";
    }
}
