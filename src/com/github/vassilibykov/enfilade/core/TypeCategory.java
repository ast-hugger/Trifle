// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

/**
 * A broad category of types as seen by the JVM, i.e. reference types vs.
 * primitive {@code int}s, vs other primitive types.
 */
public enum TypeCategory {
    REFERENCE(Object.class),
    INT(int.class),
    BOOL(boolean.class);

    public static TypeCategory ofObject(Object value) {
        if (value instanceof Integer) {
            return INT;
        } if (value instanceof Boolean) {
            return BOOL;
        } else {
            return REFERENCE;
        }
    }

    public interface Matcher<T> {
        T ifReference();
        T ifInt();
        T ifBoolean();
    }

    public interface VoidMatcher {
        void ifReference();
        void ifInt();
        void ifBoolean();
    }

    /*
        Instance
     */

    private final Class<?> representativeType;

    TypeCategory(Class<?> representativeType) {
        this.representativeType = representativeType;
    }

    public Class<?> representativeClass() {
        return representativeType;
    }

    public TypeCategory union(TypeCategory another) {
        if (this == another) {
            return this;
        } else {
            return REFERENCE;
        }
    }

    public <T> T match(Matcher<T> matcher) {
        switch (this) {
            case REFERENCE: return matcher.ifReference();
            case INT: return matcher.ifInt();
            case BOOL: return matcher.ifBoolean();
            default:
                throw new AssertionError("missing matcher case");
        }
    }

    public void match(VoidMatcher matcher) {
        switch (this) {
            case REFERENCE:
                matcher.ifReference();
                break;
            case INT:
                matcher.ifInt();
                break;
            case BOOL:
                matcher.ifBoolean();
                break;
            default:
                throw new AssertionError("missing matcher case");
        }
    }
}
