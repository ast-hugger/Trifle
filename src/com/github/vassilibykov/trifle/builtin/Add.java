// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.builtin;

import com.github.vassilibykov.trifle.core.SquarePegException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

/**
 * Implements addition on integers of unlimited size, overflowing into
 * {@link BigInteger}s as needed.
 */
public class Add extends BuiltinFunction {
    public static final Add INSTANCE = new Add();

    private Add() {
        super("add");
    }

    @Override
    public MethodHandle invoker(MethodType callSiteType) {
        var type1 = callSiteType.parameterType(0);
        var type2 = callSiteType.parameterType(1);
        if (type1 == int.class && type2 == int.class) {
            return ADD_INT.asType(callSiteType);
        } else {
            return ADD_GENERIC.asType(callSiteType);
        }
    }

    public static int add(int x, int y) {
        try {
            return Math.addExact(x, y);
        } catch (ArithmeticException e) {
            var result = BigInteger.valueOf(x).add(BigInteger.valueOf(y));
            throw SquarePegException.with(result);
        }
    }

    public static Object add(Object x, Object y) {
        try {
            return Math.addExact((Integer) x, (Integer) y);
        } catch (ClassCastException | ArithmeticException e) {
            return generalAdd(x, y);
        }
    }

    private static Object generalAdd(Object x, Object y) {
        if (x instanceof Integer) {
            if (y instanceof Integer) {
                return BigInteger.valueOf((Integer) x).add(BigInteger.valueOf((Integer) y));
            } else if (y instanceof BigInteger) {
                return BigInteger.valueOf((Integer) x).add((BigInteger) y);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (x instanceof BigInteger) {
            if (y instanceof Integer) {
                return ((BigInteger) x).add(BigInteger.valueOf((Integer) y));
            } else if (y instanceof BigInteger) {
                return ((BigInteger) x).add((BigInteger) y);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static final MethodHandle ADD_INT;
    private static final MethodHandle ADD_GENERIC;
    static {
        var lookup = MethodHandles.lookup();
        try {
            ADD_INT = lookup.findStatic(Add.class, "add",
                MethodType.methodType(int.class, int.class, int.class));
            ADD_GENERIC = lookup.findStatic(Add.class, "add",
                MethodType.methodType(Object.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
