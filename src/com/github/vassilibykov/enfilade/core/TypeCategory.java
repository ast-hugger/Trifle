// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

/**
 * A broad category of types as seen by the JVM, i.e. reference types vs.
 * primitive {@code int}s, vs other primitive types.
 */
public enum TypeCategory {
    REFERENCE(Object.class),
    INT(int.class),
    BOOL(boolean.class),
    /**
     * The type of a continuation which accepts any value, such as a non-tail
     * expression of a {@link Block}, or the type of an expression which
     * produces no value (does not call its continuation), such as the {@link
     * Ret}.
     */
    VOID(void.class);

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
        default T ifVoid() {
            // A void type category is only used in one specific case, as the type of
            // a continuation that will discard its value. We don't expect to see it in other
            // scenarios. This default method allows us to just ignore its potential existence.
            throw new AssertionError("a VOID type category is not expected here");
        }
    }

    public interface VoidMatcher {
        void ifReference();
        void ifInt();
        void ifBoolean();
        default void ifVoid() {
            // A void type category is only used in one specific case, as the type of
            // a continuation that will discard its value. We don't expect to see it in other
            // scenarios. This default method allows us to just ignore its potential existence.
            throw new AssertionError("a VOID type category is not expected here");
        }
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

    /**
     * Return a union of this type and another, with {@link #VOID} being
     * the zero: a union of it with any type is the other type.
     */
    public TypeCategory union(TypeCategory another) {
        if (this == another || another == VOID) {
            return this;
        } else if (this == VOID) {
            return another;
        } else {
            return REFERENCE;
        }
    }

    public <T> T match(Matcher<T> matcher) {
        switch (this) {
            case REFERENCE: return matcher.ifReference();
            case INT: return matcher.ifInt();
            case BOOL: return matcher.ifBoolean();
            case VOID: return matcher.ifVoid();
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
            case VOID:
                matcher.ifVoid();
                break;
            default:
                throw new AssertionError("missing matcher case");
        }
    }
}
