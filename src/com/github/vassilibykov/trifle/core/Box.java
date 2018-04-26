// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

/**
 * Part of the closures implementation. Holds the value of a variable (local or
 * a function parameter) which is mutable and has non-local references.
 */
class Box {
    static final String SET_VALUE = "setValue";
    static final String VALUE_AS_REFERENCE = "valueAsReference";
    static final String VALUE_AS_INT = "valueAsInt";

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

    @SuppressWarnings("unused") // called by generated code; see references to VALUE_AS_REFERENCE constant
    synchronized Object valueAsReference() {
        return referenceValue != NO_VALUE ? referenceValue : intValue;
    }

    @SuppressWarnings("unused") // called by generated code; see references to VALUE_AS_INT constant
    synchronized int valueAsInt() {
        if (referenceValue == NO_VALUE) return intValue;
        if (referenceValue instanceof Integer) return (Integer) referenceValue;
        throw SquarePegException.with(referenceValue);
    }

    synchronized void setValue(Object value) {
        if (value instanceof Integer) {
            referenceValue = NO_VALUE;
            intValue = (Integer) value;
        } else {
            referenceValue = value;
        }
    }

    synchronized void setValue(int value) {
        referenceValue = NO_VALUE;
        intValue = value;
    }
}
