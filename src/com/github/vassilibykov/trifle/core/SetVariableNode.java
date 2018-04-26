// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

/**
 * An expression mutating a variable to have a new value. The new value, which
 * is also the value of this expression, must be an atomic expression.
 */
class SetVariableNode extends EvaluatorNode implements RecoverySite {
    @NotNull private AbstractVariable variable;
    @NotNull private final EvaluatorNode value;
    private Label recoverySiteLabel;

    SetVariableNode(@NotNull VariableDefinition variable, @NotNull EvaluatorNode value) {
        this.variable = variable;
        this.value = value;
    }

    public AbstractVariable variable() {
        return variable;
    }

    void replaceVariable(@NotNull AbstractVariable variable) {
        this.variable = variable;
    }

    public EvaluatorNode value() {
        return value;
    }

    @Override
    public Label recoverySiteLabel() {
        return recoverySiteLabel;
    }

    @Override
    public void setRecoverySiteLabel(Label recoverySiteLabel) {
        this.recoverySiteLabel = recoverySiteLabel;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSetVar(this);
    }

    @Override
    public String toString() {
        return "(set! " + variable + " " + value + ")";
    }
}
