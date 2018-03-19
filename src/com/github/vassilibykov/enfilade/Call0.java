package com.github.vassilibykov.enfilade;

public class Call0 extends Call {

    Call0(Function function) {
        super(function);
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
