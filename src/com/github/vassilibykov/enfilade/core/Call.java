// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public abstract class Call extends ComplexExpression {
    @NotNull private Function function;
    /*internal*/ final ValueProfile profile = new ValueProfile();

    Call(@NotNull Function function) {
        if (function.arity() != arity()) {
            throw new AssertionError("a function of arity " + arity() + " required");
        }
        this.function = function;
    }

    public Function function() {
        return function;
    }

    protected abstract int arity();
}
