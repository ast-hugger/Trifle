// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

class BoxedBool {
    static BoxedBool with(boolean value) {
        return new BoxedBool(value);
    }

    boolean value;

    private BoxedBool(boolean value) {
        this.value = value;
    }
}
