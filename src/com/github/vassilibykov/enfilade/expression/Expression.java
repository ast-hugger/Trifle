// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

public abstract class Expression {
    public abstract <T> T accept(Visitor<T> visitor);
}
