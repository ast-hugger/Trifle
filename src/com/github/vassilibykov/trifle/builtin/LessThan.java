// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.builtin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

public class LessThan extends BuiltinFunction {
    public static final LessThan INSTANCE = new LessThan();

    private static final MethodHandle LESS_THAN_INT;
    private static final MethodHandle LESS_THAN_GENERIC;
    static {
        var lookup = MethodHandles.lookup();
        try {
            LESS_THAN_INT = lookup.findStatic(LessThan.class, "lessThan",
                MethodType.methodType(boolean.class, int.class, int.class));
            LESS_THAN_GENERIC = lookup.findStatic(LessThan.class, "lessThan",
                MethodType.methodType(boolean.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private LessThan() {
        super("trifle:LT");
    }

    @Override
    public MethodHandle invoker(MethodType type) {
        var type1 = type.parameterType(0);
        var type2 = type.parameterType(1);
        if (type1 == int.class && type2 == int.class) {
            return LESS_THAN_INT.asType(type);
        } else {
            return LESS_THAN_GENERIC.asType(type);
        }
    }

    public static boolean lessThan(int x, int y) {
        return x < y;
    }

    public static boolean lessThan(Object x, Object y) {
        try {
            return (Integer) x < (Integer) y;
        } catch (ClassCastException e) {
            return generalLessThan(x, y);
        }
    }

    private static boolean generalLessThan(Object x, Object y) {
        if (x instanceof Integer) {
            if (y instanceof Integer) {
                return (Integer) x < (Integer) y;
            } else if (y instanceof BigInteger) {
                return BigInteger.valueOf((Integer) x).compareTo((BigInteger) y) < 0;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (x instanceof BigInteger) {
            if (y instanceof Integer) {
                return ((BigInteger) x).compareTo(BigInteger.valueOf((Integer) y)) < 0;
            } else if (y instanceof BigInteger) {
                return ((BigInteger) x).compareTo((BigInteger) y) < 0;
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
