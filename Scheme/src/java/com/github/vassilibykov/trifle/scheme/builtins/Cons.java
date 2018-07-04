// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme.builtins;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import com.github.vassilibykov.trifle.scheme.Pair;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Cons extends BuiltinFunction {
    public static final Cons INSTANCE = new Cons();

    private static final MethodHandle REF_CONS;
    private static final MethodHandle INT_CONS;
    static {
        var lookup = MethodHandles.lookup();
        try {
            REF_CONS = lookup.findStatic(
                Cons.class, "refCons", MethodType.methodType(Object.class, Object.class, Object.class));
            INT_CONS = lookup.findStatic(
                Cons.class, "intCons", MethodType.methodType(Object.class, int.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private Cons() {
        super("scheme:cons");
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        if (methodType.parameterType(0) == int.class) {
            return INT_CONS.asType(methodType);
        } else {
            return REF_CONS.asType(methodType);
        }
    }

    private static Object refCons(Object car, Object cdr) {
        return Pair.of(car, cdr);
    }

    private static Object intCons(int car, Object cdr) {
        return Pair.of(car, cdr);
    }
}
