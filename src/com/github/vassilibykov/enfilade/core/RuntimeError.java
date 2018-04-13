// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

/**
 * A runtime error which indicated a problem in code being executed.
 */
public class RuntimeError extends RuntimeException {
    public static final String INTERNAL_CLASS_NAME = GhostWriter.internalClassName(RuntimeError.class);

    public static RuntimeError message(String message) {
        return new RuntimeError(message);
    }

    public static RuntimeError booleanExpected() {
        return message("boolean expected");
    }

    public static RuntimeError integerExpected() {
        return message("integer expected");
    }

    private RuntimeError(String message) {
        super(message);
    }

}
