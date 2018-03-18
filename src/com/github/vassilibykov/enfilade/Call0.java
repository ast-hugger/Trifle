package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

public class Call0 extends Call {

    Call0(Method method) {
        super(method);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall0(this);
    }

    @Override
    protected int arity() {
        return 0;
    }
}
