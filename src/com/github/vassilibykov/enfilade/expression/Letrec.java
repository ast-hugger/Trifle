// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

public class Letrec extends Let {
    public static Letrec with(Variable variable, Expression initializer, Expression body) {
        return new Letrec(variable, initializer, body);
    }

    Letrec(@NotNull Variable variable, @NotNull Expression initializer, @NotNull Expression body) {
        super(variable, initializer, body);
    }

    @Override
    public String toString() {
        return "letrec(" + variable() + " " + body() + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLetrec(this);
    }
}
