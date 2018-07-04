// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme.builtins;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import com.github.vassilibykov.trifle.scheme.Pair;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class PairP extends BuiltinFunction {
    public static final PairP INSTANCE = new PairP();

    private static final MethodHandle IS_PAIR;
    private static final MethodHandle IS_PAIR_INT;
    static {
        var lookup = MethodHandles.lookup();
        try {
            IS_PAIR = lookup.findStatic(PairP.class, "isPair", MethodType.methodType(Object.class, Object.class));
            IS_PAIR_INT = lookup.findStatic(PairP.class, "isPair", MethodType.methodType(boolean.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private PairP() {
        super("scheme:pair?");
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        if (methodType.parameterType(0) == int.class) {
            return IS_PAIR_INT.asType(methodType);
        } else {
            return IS_PAIR.asType(methodType);
        }
    }

    private static Object isPair(Object arg) {
        return arg instanceof Pair;
    }

    private static boolean isPair(int arg) {
        return false;
    }
}
