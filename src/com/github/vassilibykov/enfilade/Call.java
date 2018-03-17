package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

public abstract class Call extends ComplexExpression {
    @NotNull private Method method;

    Call(@NotNull Method method) {
        this.method = method;
    }

    public Method method() {
        return method;
    }
}
