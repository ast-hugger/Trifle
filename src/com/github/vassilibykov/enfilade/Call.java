package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

public abstract class Call extends ComplexExpression {
    @NotNull private Function function;
    /*internal*/ final ValueProfile profile = new ValueProfile();

    Call(@NotNull Function function) {
        if (function.arity() != arity()) {
            throw new AssertionError("a function of arity " + arity() + " required");
        }
        this.function = function;
    }

    public Function function() {
        return function;
    }

    protected abstract int arity();
}
