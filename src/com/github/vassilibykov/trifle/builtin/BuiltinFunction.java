// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.builtin;

import com.github.vassilibykov.trifle.core.FreeFunction;
import org.jetbrains.annotations.NotNull;

public abstract class BuiltinFunction implements FreeFunction {
    @NotNull private final String name;

    protected BuiltinFunction(@NotNull String name) {
        this.name = name;
        Builtins.register(this);
    }

    @Override
    public String name() {
        return name;
    }
}
