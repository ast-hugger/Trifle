// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme.builtins;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import com.github.vassilibykov.trifle.scheme.Pair;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Cdr extends BuiltinFunction {
    public static final Cdr INSTANCE = new Cdr();

    private static final MethodHandle CDR;
    static {
        var lookup = MethodHandles.lookup();
        try {
            CDR = lookup.findStatic(Cdr.class, "cdr", MethodType.methodType(Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /*
        Instance
     */

    private Cdr() {
        super("scheme:cdr");
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        return CDR.asType(methodType);
    }

    private static Object cdr(Object object) {
        if (!(object instanceof Pair)) {
            throw new IllegalArgumentException("cdr() argument is not a pair: " + object);
        }
        return ((Pair) object).cdr();
    }
}
