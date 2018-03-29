// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

public class LetRec extends Let {
    public static LetRec with(Variable variable, Expression initializer, Expression body) {
        return new LetRec(variable, initializer, body);
    }

    LetRec(@NotNull Variable variable, @NotNull Expression initializer, @NotNull Expression body) {
        super(variable, initializer, body);
    }

    @Override
    public String toString() {
        return "letrec(" + variable() + " " + body() + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLetRec(this);
    }
}
