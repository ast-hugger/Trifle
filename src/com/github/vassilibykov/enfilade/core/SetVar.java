package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class SetVar extends ComplexExpression {
    @NotNull private final Var variable;
    @NotNull private final AtomicExpression value;

    SetVar(@NotNull Var variable, @NotNull AtomicExpression value) {
        this.variable = variable;
        this.value = value;
    }

    public Var variable() {
        return variable;
    }

    public AtomicExpression value() {
        return value;
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
