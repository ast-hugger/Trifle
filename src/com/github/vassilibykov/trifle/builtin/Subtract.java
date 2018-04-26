// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.builtin;

import com.github.vassilibykov.trifle.core.SquarePegException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

/**
 * Implements subtraction on integers of unlimited size, overflowing into
 * {@link BigInteger}s as needed.
 */
public class Subtract extends BuiltinFunction {
    public static final Subtract INSTANCE = new Subtract();

    private Subtract() {
        super("subtract");
    }

    @Override
    public MethodHandle invoker(MethodType callSiteType) {
        var type1 = callSiteType.parameterType(0);
        var type2 = callSiteType.parameterType(1);
        if (type1 == int.class && type2 == int.class) {
            return SUBSTRACT_INT.asType(callSiteType);
        } else {
            return SUBTRACT_GENERIC.asType(callSiteType);
        }
    }

    public static int subtract(int x, int y) {
        try {
            return Math.subtractExact(x, y);
        } catch (ArithmeticException e) {
            var result = BigInteger.valueOf(x).subtract(BigInteger.valueOf(y));
            throw SquarePegException.with(result);
        }
    }

    public static Object subtract(Object x, Object y) {
        try {
            return Math.subtractExact((Integer) x, (Integer) y);
        } catch (ClassCastException | ArithmeticException e) {
            return generalSubstract(x, y);
        }
    }

    private static Object generalSubstract(Object x, Object y) {
        if (x instanceof Integer) {
            if (y instanceof Integer) {
                return BigInteger.valueOf((Integer) x).subtract(BigInteger.valueOf((Integer) y));
            } else if (y instanceof BigInteger) {
                return BigInteger.valueOf((Integer) x).subtract((BigInteger) y);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (x instanceof BigInteger) {
            if (y instanceof Integer) {
                return ((BigInteger) x).subtract(BigInteger.valueOf((Integer) y));
            } else if (y instanceof BigInteger) {
                return ((BigInteger) x).subtract((BigInteger) y);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static final MethodHandle SUBSTRACT_INT;
    private static final MethodHandle SUBTRACT_GENERIC;
    static {
        var lookup = MethodHandles.lookup();
        try {
            SUBSTRACT_INT = lookup.findStatic(Subtract.class, "subtract",
                MethodType.methodType(int.class, int.class, int.class));
            SUBTRACT_GENERIC = lookup.findStatic(Subtract.class, "subtract",
                MethodType.methodType(Object.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
