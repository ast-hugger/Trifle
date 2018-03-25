// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.Nullable;

/**
 * A variable definition or reference. For simplicity, in the definition
 * language we conflate these two concepts in a single class. For execution,
 * they will be separated as needed.
 */
public class Variable extends AtomicExpression {
    public static Variable named(String name) {
        return new Variable(name);
    }

    @Nullable private final String name;

    private Variable(@Nullable String name) {
        this.name = name;
    }

    public String name() {
        return name != null ? name : "var" + hashCode();
    }

    @Override
    public String toString() {
        return "var(" + name() + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVariable(this);
    }
}
