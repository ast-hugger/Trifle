// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class CompilerError extends RuntimeException {
    public CompilerError(String message) {
        super(message);
    }
}
