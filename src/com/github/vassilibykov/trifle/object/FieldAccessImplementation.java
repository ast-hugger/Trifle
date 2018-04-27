// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import org.objectweb.asm.Handle;

/**
 * Describes a particular implementation of {@link FixedObject FixedObject's}
 * field access invokedynamic instructions. An instance currently in force is
 * available as {@link FixedObject#accessImplementation()}. Allows selecting a
 * custom instrumented implementation for unit tests.
 */
public interface FieldAccessImplementation {
    static String getterName(String fieldName) {
        return "get|" + fieldName;
    }

    static String setterName(String fieldName) {
        return "set|" + fieldName;
    }

    static String extractFieldName(String indyName) {
        var index = indyName.indexOf('|');
        if (index < 0) throw new AssertionError();
        return indyName.substring(index + 1);
    }

    Handle getterBootstrapper();
    Handle setterBootstrapper();
}
