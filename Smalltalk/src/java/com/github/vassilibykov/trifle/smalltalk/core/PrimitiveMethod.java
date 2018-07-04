// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.core.Invocable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

abstract class PrimitiveMethod implements Invocable {

    private static final MethodHandle INVOKE0;
    private static final MethodHandle INVOKE1;
    private static final MethodHandle INVOKE2;
    static {
        var lookup = MethodHandles.lookup();
        try {
            INVOKE0 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class));
            INVOKE1 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class, Object.class));
            INVOKE2 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Object invoke() {
        throw new RuntimeException("invalid number of arguments");
    }

    @Override
    public Object invoke(Object o) {
        throw new RuntimeException("invalid number of arguments");
    }

    @Override
    public Object invoke(Object o, Object o1) {
        throw new RuntimeException("invalid number of arguments");
    }

    @Override
    public Object invokeWithArguments(Object[] objects) {
        switch (objects.length) {
            case 0:
                return invoke();
            case 1:
                return invoke(objects[0]);
            case 2:
                return invoke(objects[0], objects[1]);
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        switch (methodType.parameterCount()) {
            case 0:
                return INVOKE0.bindTo(this);
            case 1:
                return INVOKE1.bindTo(this);
            case 2:
                return INVOKE2.bindTo(this);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
