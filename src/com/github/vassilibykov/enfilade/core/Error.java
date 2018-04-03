// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

/**
 * A runtime error which indicated a problem in code being executed.
 */
public class Error extends RuntimeException {
    public static final String INTERNAL_CLASS_NAME = GhostWriter.internalClassName(Error.class);

    public static Error message(String message) {
        return new Error(message);
    }

    public static Error booleanExpected() {
        return message("boolean expected");
    }

    public static Error integerExpected() {
        return message("integer expected");
    }

    private Error(String message) {
        super(message);
    }

}
