// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * The core API of an object encapsulating executable code. The API includes
 * methods for invoking the executable code, with 0-5 arguments as well as with
 * an array containing an arbitrary number of arguments. An invocation may fail
 * if the executable code is not prepared to handle that particular number of
 * arguments. An implementation must guarantee that for the 0-5 argument case
 * an invocation via one of the {@link #invoke} methods has the same effect
 * as an invocation via the {@link #invokeWithArguments(Object[])} method.
 */
public interface Invocable {
    Object invoke();
    Object invoke(Object arg);
    Object invoke(Object arg1, Object arg2);
    Object invoke(Object arg1, Object arg2, Object arg3);
    Object invoke(Object arg1, Object arg2, Object arg3, Object arg4);
    Object invokeWithArguments(Object[] arguments);
    MethodHandle invoker(MethodType type);
}
