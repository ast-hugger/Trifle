// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

class NonLocalReturnException extends RuntimeException {

    final Object value;

    NonLocalReturnException(Object value) {
        this.value = value;
    }
}
