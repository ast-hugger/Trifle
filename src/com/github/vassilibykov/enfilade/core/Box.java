// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class Box {
    public static Box with(Object value) {
        return new Box(value);
    }

    public Object value;

    private Box(Object value) {
        this.value = value;
    }
}
