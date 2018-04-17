// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.lang.invoke.MethodType;

/**
 * Part of the closures implementation. Hold the value of a variable (local or
 * a function parameter) which is mutable and has non-local references.
 */
class Box {
    static final String INTERNAL_CLASS_NAME = GhostWriter.internalClassName(Box.class);
    static final String SET_VALUE_NAME = "setValue";
    static final String SET_VALUE_REFERENCE_DESC = MethodType.methodType(void.class, Object.class).toMethodDescriptorString();
    static final String SET_VALUE_INT_DESC = MethodType.methodType(void.class, int.class).toMethodDescriptorString();

    private static final Object NO_VALUE = new Object();

    static Box with(Object object) {
        return object instanceof Integer
            ? new Box(NO_VALUE, (Integer) object)
            : new Box(object, 0);
    }

    static Box with(int value) {
        return new Box(NO_VALUE, value);
    }

    private Object referenceValue;
    private int intValue;

    private Box(Object referenceValue, int intValue) {
        this.referenceValue = referenceValue;
        this.intValue = intValue;
    }

    @SuppressWarnings("unused") // called by generated code
    Object valueAsReference() {
        return referenceValue != NO_VALUE ? referenceValue : intValue;
    }

    @SuppressWarnings("unused") // called by generated code
    int valueAsInt() {
        if (referenceValue == NO_VALUE) return intValue;
        if (referenceValue instanceof Integer) return (Integer) referenceValue;
        throw SquarePegException.with(referenceValue);
    }

    void setValue(Object value) {
        if (value instanceof Integer) {
            referenceValue = NO_VALUE;
            intValue = (Integer) value;
        } else {
            referenceValue = value;
        }
    }

    void setValue(int value) {
        referenceValue = NO_VALUE;
        intValue = value;
    }
}
