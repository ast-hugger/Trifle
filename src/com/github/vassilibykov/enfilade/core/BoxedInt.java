// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

class BoxedInt {
    static BoxedInt with(int value) {
        return new BoxedInt(value);
    }

    int value;

    private BoxedInt(int value) {
        this.value = value;
    }
}
