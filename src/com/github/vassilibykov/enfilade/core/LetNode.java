// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * Represents both {@code let} and {@code letrec} of the expression language.
 */
class LetNode extends EvaluatorNode implements RecoverySite {
    @NotNull private final VariableDefinition variable;
    @NotNull private final EvaluatorNode initializer;
    @NotNull private final EvaluatorNode body;
    private int setInstructionAddress;

    LetNode(@NotNull VariableDefinition variable, @NotNull EvaluatorNode initializer, @NotNull EvaluatorNode body) {
        this.variable = variable;
        this.initializer = initializer;
        this.body = body;
    }

    public VariableDefinition variable() {
        return variable;
    }

    public EvaluatorNode initializer() {
        return initializer;
    }

    public EvaluatorNode body() {
        return body;
    }

    @Override
    public int resumptionAddress() {
        return setInstructionAddress;
    }

    @Override
    public void setResumptionAddress(int resumptionAddress) {
        this.setInstructionAddress = resumptionAddress;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLet(this);
    }

    @Override
    public String toString() {
        return "(let (" + variable() + " ...) ...)";
    }
}
