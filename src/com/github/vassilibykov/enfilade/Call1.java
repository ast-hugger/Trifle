package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

public class Call1 extends Call {
    @NotNull private final AtomicExpression arg;

    Call1(Method method, @NotNull AtomicExpression arg) {
        super(method);
        this.arg = arg;
    }

    public AtomicExpression arg() {
        return arg;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall1(this);
    }
}
