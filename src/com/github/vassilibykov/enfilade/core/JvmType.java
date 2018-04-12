// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A broad category of types as seen by the JVM, i.e. reference types vs.
 * primitive {@code int}s, vs other primitive types.
 */
public enum JvmType {
    REFERENCE(Object.class),
    INT(int.class),
    BOOL(boolean.class),
    /**
     * The type of a continuation which accepts any value, such as a non-tail
     * expression of a {@link BlockNode}, or the type of an expression which
     * produces no value (does not call its continuation), such as the {@link
     * ReturnNode}.
     */
    VOID(void.class);

    public static JvmType ofObject(Object value) {
        if (value instanceof Integer) {
            return INT;
        } else if (value instanceof Boolean) {
            return BOOL;
        } else {
            return REFERENCE;
        }
    }

    public static boolean isCompatibleValue(Class<?> typeToken, Object value) {
        /* This method has nothing to do with JvmType directly, but its logic depends on
           the set of primitive types enumerated by JvmType and it needs to be updated if
           those ever change. So, it's better to keep it here. */
        if (typeToken == int.class && !(value instanceof Integer)) return false;
        if (typeToken == boolean.class && !(value instanceof Boolean)) return false;
        return true;
    }

    public interface Matcher<T> {
        T ifReference();
        T ifInt();
        T ifBoolean();
        default T ifVoid() {
            // A void type category is only used in one specific case, as the type of
            // a continuation that will discard its value. We don't expect to see it in other
            // scenarios. This default method allows us to just ignore its potential existence.
            throw new AssertionError("a VOID type is not expected here");
        }
    }

    public interface VoidMatcher {
        void ifReference();
        void ifInt();
        void ifBoolean();
        default void ifVoid() {
            // A void type is only used in one specific case, as the type of
            // a continuation that will discard its value. We don't expect to see it in other
            // scenarios. This default method allows us to just ignore its potential existence.
            throw new AssertionError("a VOID type is not expected here");
        }
    }

    public static MethodHandle adaptToCallSite(MethodType callSiteType, MethodHandle original) {
        if (original.type().equals(callSiteType)) return original;
        if (!original.type().returnType().isPrimitive() && callSiteType.returnType().isPrimitive()) {
            var genericReturn = callSiteType.changeReturnType(Object.class);
            var invoker = original.asType(genericReturn);
            MethodHandle filtered = guardReturnValue(callSiteType.returnType(), invoker);
            return filtered.asType(callSiteType);
        } else {
            return original.asType(callSiteType);
        }
    }

    /**
     * Wrap a method handle so that its return value is checked for definite
     * ability to be converted to the expected type, throwing an SPE if the
     * value is incompatible.
     */
    public static MethodHandle guardReturnValue(Class<?> expectedReturnType, MethodHandle producer) {
        if (expectedReturnType.isPrimitive()) {
            return MethodHandles.filterReturnValue(
                producer,
                ENSURE_UNBOXABLE_VALUE.bindTo(primitiveToWrapper(expectedReturnType)));
        } else {
            return producer;
        }
    }

    private static final MethodHandle ENSURE_UNBOXABLE_VALUE;
    static {
        try {
            ENSURE_UNBOXABLE_VALUE = MethodHandles.lookup()
                .findStatic(
                    JvmType.class,
                    "ensureUnboxableValue",
                    MethodType.methodType(Object.class, Class.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError();
        }
    }

    private static Object ensureUnboxableValue(Class<?> expectedType, Object value) {
        if (expectedType.isInstance(value)) return value;
        throw SquarePegException.with(value);
    }

    private static Class<?> primitiveToWrapper(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == boolean.class) return Boolean.class;
        throw new AssertionError("unsupported primitive type");
    }



    /*
        Instance
     */

    private final Class<?> representativeType;

    JvmType(Class<?> representativeType) {
        this.representativeType = representativeType;
    }

    public Class<?> representativeClass() {
        return representativeType;
    }

    /**
     * Return a union of this type and another, with {@link #VOID} being
     * the zero: a union of it with any type is the other type.
     */
    public JvmType union(JvmType another) {
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
                throw new AssertionError("no match() method case for " + this);
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
                throw new AssertionError("no match() method case for " + this);
        }
    }
}
