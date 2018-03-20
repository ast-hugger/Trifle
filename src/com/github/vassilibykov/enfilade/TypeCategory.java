package com.github.vassilibykov.enfilade;

/**
 * A broad category of types as seen by the JVM, i.e. reference types vs.
 * primitive {@code int}s, vs other primitive types.
 */
public enum TypeCategory {
    REFERENCE(Object.class),
    INT(int.class);

    public static TypeCategory ofObject(Object value) {
        if (value instanceof Integer) {
            return INT;
        } else {
            return REFERENCE;
        }
    }

    /*
        Instance
     */

    private final Class<?> representativeType;

    TypeCategory(Class<?> representativeType) {
        this.representativeType = representativeType;
    }

    public Class<?> representativeType() {
        return representativeType;
    }

    public TypeCategory union(TypeCategory another) {
        if (this == another) {
            return this;
        } else {
            return REFERENCE;
        }
    }
}
