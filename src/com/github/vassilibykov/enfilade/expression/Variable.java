// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

/**
 * A variable definition or reference. For simplicity, in the definition
 * language we conflate these two concepts in a single class. For execution,
 * they will be separated as needed.
 */
public class Variable extends AtomicExpression {
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVariable(this);
    }
}
