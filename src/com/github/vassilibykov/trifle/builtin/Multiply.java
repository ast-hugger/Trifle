// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.builtin;

import com.github.vassilibykov.trifle.core.SquarePegException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

/**
 * Implements multiplication on integers of unlimited size, overflowing into
 * {@link BigInteger}s as needed.
 */
public class Multiply extends BuiltinFunction {
    public static final Multiply INSTANCE = new Multiply();

    private Multiply() {
        super("multiply");
    }

    @Override
    public MethodHandle invoker(MethodType callSiteType) {
        var type1 = callSiteType.parameterType(0);
        var type2 = callSiteType.parameterType(1);
        if (type1 == int.class && type2 == int.class) {
            return MULTIPLY_INT.asType(callSiteType);
        } else {
            return MULTIPLY_GENERIC.asType(callSiteType);
        }
    }

    public static int multiply(int x, int y) {
        try {
            return Math.multiplyExact(x, y);
        } catch (ArithmeticException e) {
            var result = BigInteger.valueOf(x).multiply(BigInteger.valueOf(y));
            throw SquarePegException.with(result);
        }
    }

    public static Object multiply(Object x, Object y) {
        try {
            return Math.multiplyExact((Integer) x, (Integer) y);
        } catch (ClassCastException | ArithmeticException e) {
            return generalMultiply(x, y);
        }
    }

    private static Object generalMultiply(Object x, Object y) {
        if (x instanceof Integer) {
            if (y instanceof Integer) {
                return BigInteger.valueOf((Integer) x).multiply(BigInteger.valueOf((Integer) y));
            } else if (y instanceof BigInteger) {
                return BigInteger.valueOf((Integer) x).multiply((BigInteger) y);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (x instanceof BigInteger) {
            if (y instanceof Integer) {
                return ((BigInteger) x).multiply(BigInteger.valueOf((Integer) y));
            } else if (y instanceof BigInteger) {
                return ((BigInteger) x).multiply((BigInteger) y);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static final MethodHandle MULTIPLY_INT;
    private static final MethodHandle MULTIPLY_GENERIC;
    static {
        var lookup = MethodHandles.lookup();
        try {
            MULTIPLY_INT = lookup.findStatic(Multiply.class, "multiply",
                MethodType.methodType(int.class, int.class, int.class));
            MULTIPLY_GENERIC = lookup.findStatic(Multiply.class, "multiply",
                MethodType.methodType(Object.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
