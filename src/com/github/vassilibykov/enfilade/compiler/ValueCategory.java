package com.github.vassilibykov.enfilade.compiler;

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
}
