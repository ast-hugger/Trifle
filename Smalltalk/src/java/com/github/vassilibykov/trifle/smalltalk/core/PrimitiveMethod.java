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
    private static final MethodHandle INVOKE3;
    private static final MethodHandle INVOKE4;
    private static final MethodHandle INVOKE_N;
    static {
        var lookup = MethodHandles.lookup();
        try {
            INVOKE0 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class));
            INVOKE1 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class, Object.class));
            INVOKE2 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class, Object.class, Object.class));
            INVOKE3 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class, Object.class, Object.class, Object.class));
            INVOKE4 = lookup.findVirtual(PrimitiveMethod.class,"invoke", MethodType.methodType(Object.class, Object.class, Object.class, Object.class, Object.class));
            INVOKE_N = lookup.findVirtual(PrimitiveMethod.class, "invokeN", MethodType.methodType(Object.class, Object[].class));
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
    public Object invoke(Object o, Object o1, Object o2) {
        throw new RuntimeException("invalid number of arguments");
    }

    @Override
    public Object invoke(Object o, Object o1, Object o2, Object o3) {
        throw new RuntimeException("invalid number of arguments");
    }

    protected Object invokeN(Object[] args) {
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
            case 3:
                return invoke(objects[0], objects[1], objects[2]);
            case 4:
                return invoke(objects[0], objects[1], objects[2], objects[3]);
            default:
                return invokeN(objects);
        }
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        switch (methodType.parameterCount()) {
            case 0:
                return INVOKE0.bindTo(this); // FIXME this is impossible
            case 1:
                return INVOKE1.bindTo(this);
            case 2:
                return INVOKE2.bindTo(this);
            case 3:
                return INVOKE3.bindTo(this);
            case 4:
                return INVOKE4.bindTo(this);
            default:
                return INVOKE_N.asCollector(Object[].class, methodType.parameterCount()).bindTo(this);
        }
    }
}
