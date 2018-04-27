// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public interface Invocable {
    Object invoke();
    Object invoke(Object arg);
    Object invoke(Object arg1, Object arg2);
    Object invokeWithArguments(Object[] arguments);
    MethodHandle invoker(MethodType type);
}
