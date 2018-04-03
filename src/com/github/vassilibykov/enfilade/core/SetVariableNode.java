// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * An expression mutating a variable to have a new value. The new value, which
 * is also the value of this expression, must be an atomic expression.
 */
class SetVariableNode extends EvaluatorNode implements RecoverySite {
    @NotNull private AbstractVariable variable;
    @NotNull private final EvaluatorNode value;
    private int setInstructionAddress;

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
    public int resumptionAddress() {
        return setInstructionAddress;
    }

    @Override
    public void setResumptionAddress(int resumptionAddress) {
        setInstructionAddress = resumptionAddress;
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
