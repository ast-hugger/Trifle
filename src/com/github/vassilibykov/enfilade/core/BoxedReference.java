// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

class BoxedReference {
    static BoxedReference with(Object value) {
        return new BoxedReference(value);
    }

    Object value;

    private BoxedReference(Object value) {
        this.value = value;
    }
}
