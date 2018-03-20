package com.github.vassilibykov.enfilade.core;

import java.util.function.Supplier;

/**
 * A broad category of types as seen by the JVM, i.e. reference types vs.
 * primitive {@code int}s, vs other primitive types.
 */
public enum TypeCategory {
    REFERENCE(Object.class),
    INT(int.class),
    BOOLEAN(boolean.class);

    public static TypeCategory ofObject(Object value) {
        if (value instanceof Integer) {
            return INT;
        } if (value instanceof Boolean) {
            return BOOLEAN;
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

    public <T> T match(Matcher<T> matcher) {
        switch (this) {
            case REFERENCE: return matcher.ifReference();
            case INT: return matcher.ifInt();
            case BOOLEAN: return matcher.ifBoolean();
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
            case BOOLEAN:
                matcher.ifBoolean();
                break;
            default:
                throw new AssertionError("missing matcher case");
        }
    }
}
