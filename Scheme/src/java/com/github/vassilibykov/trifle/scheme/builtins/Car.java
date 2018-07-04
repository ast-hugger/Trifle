// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme.builtins;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import com.github.vassilibykov.trifle.scheme.Pair;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Car extends BuiltinFunction {
    public static final Car INSTANCE = new Car();

    private static final MethodHandle REF_CAR;
    private static final MethodHandle INT_CAR;
    static {
        var lookup = MethodHandles.lookup();
        try {
            REF_CAR = lookup.findStatic(Car.class, "refCar", MethodType.methodType(Object.class, Object.class));
            INT_CAR = lookup.findStatic(Car.class, "intCar", MethodType.methodType(int.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /*
        Instance
     */

    private Car() {
        super("scheme:car");
    }

    @Override
    public MethodHandle invoker(MethodType methodType) {
        if (methodType.returnType() == int.class) {
            return INT_CAR.asType(methodType);
        } else if (methodType.returnType() == Object.class) {
            return REF_CAR.asType(methodType);
        } else {
            throw new IllegalArgumentException("invalid car() call method type: " + methodType);
        }
    }

    private static Object refCar(Object object) {
        if (!(object instanceof Pair)) {
            throw new IllegalArgumentException("car() argument is not a pair: " + object);
        }
        return ((Pair) object).car();
    }

    private static int intCar(Object object) {
        if (!(object instanceof Pair)) {
            throw new IllegalArgumentException("car() argument is not a pair: " + object);
        }
        return ((Pair) object).intCar();
    }
}
