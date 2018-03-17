package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

public class Call2 extends Call {
    @NotNull private final AtomicExpression arg1;
    @NotNull private final AtomicExpression arg2;

    Call2(Method method, @NotNull AtomicExpression arg1, @NotNull AtomicExpression arg2) {
        super(method);
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public AtomicExpression arg1() {
        return arg1;
    }

    public AtomicExpression arg2() {
        return arg2;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall2(this);
    }
}
