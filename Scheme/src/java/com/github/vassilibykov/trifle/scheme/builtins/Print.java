// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme.builtins;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Print extends BuiltinFunction {
    public static final Print INSTANCE = new Print();

    private static final MethodHandle PRINT;
    static {
        try {
            PRINT = MethodHandles.lookup().findStatic(
                Print.class, "print", MethodType.methodType(Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private Print() {
        super("scheme:print");
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        if (!PRINT.type().equals(methodType)) {
            throw new IllegalArgumentException();
        }
        return PRINT;
    }

    private static Object print(Object arg) {
        System.out.println(arg);
        return arg;
    }
}
