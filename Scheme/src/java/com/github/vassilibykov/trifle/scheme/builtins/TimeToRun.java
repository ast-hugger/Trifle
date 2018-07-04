// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme.builtins;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import com.github.vassilibykov.trifle.core.Invocable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TimeToRun extends BuiltinFunction {
    public static final TimeToRun INSTANCE = new TimeToRun();

    private static final MethodHandle TIME_TO_RUN;
    static {
        try {
            TIME_TO_RUN = MethodHandles.lookup().findStatic(
                TimeToRun.class, "timeToRun", MethodType.methodType(int.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private TimeToRun() {
        super("scheme:time-to-run");
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        if (methodType.parameterCount() != 1) {
            throw new IllegalArgumentException();
        }
        return TIME_TO_RUN.asType(methodType);
    }

    private static int timeToRun(Object invocable) {
        var function = (Invocable) invocable;
        long start = System.nanoTime();
        function.invoke();
        var elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        return (int) elapsedMs;
    }


}
