package com.github.vassilibykov.enfilade;

public enum ValueCategory {
    REFERENCE,
    INT;

    public static ValueCategory ofObject(Object value) {
        if (value instanceof Integer) {
            return INT;
        } else {
            return REFERENCE;
        }
    }

    public ValueCategory union(ValueCategory another) {
        if (this == another) {
            return this;
        } else {
            return REFERENCE;
        }
    }
}
