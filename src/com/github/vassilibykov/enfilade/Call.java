package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

public abstract class Call extends ComplexExpression {
    @NotNull private Method method;

    Call(@NotNull Method method) {
        if (method.arity() != arity()) {
            throw new AssertionError("a method of arity " + arity() + " required");
        }
        this.method = method;
    }

    public Method method() {
        return method;
    }

    protected abstract int arity();
}
