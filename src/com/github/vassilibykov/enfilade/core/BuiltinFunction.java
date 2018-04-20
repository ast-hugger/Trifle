// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevelFunction;

public abstract class BuiltinFunction implements TopLevelFunction, Callable {
    private int id = -1;

    @Override
    public int id() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    @Override
    public Callable asCallable() {
        return this;
    }
}
