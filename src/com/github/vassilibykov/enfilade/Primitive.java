package com.github.vassilibykov.enfilade;

public class Primitive extends AtomicExpression {
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPrimitive(this);
    }
}
