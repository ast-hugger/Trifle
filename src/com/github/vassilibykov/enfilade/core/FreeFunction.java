// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * A top-level user-defined or built-in function; one with no outer lexical
 * context.
 */
public interface FreeFunction extends Invocable {

    MethodHandle invoker(MethodType callSiteType);

    @Override
    default Object invoke() {
        var invoker = invoker(MethodType.genericMethodType(0));
        try {
            return invoker.invoke();
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    @Override
    default Object invoke(Object arg) {
        var invoker = invoker(MethodType.genericMethodType(1));
        try {
            return invoker.invoke(arg);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    @Override
    default Object invoke(Object arg1, Object arg2) {
        var invoker = invoker(MethodType.genericMethodType(2));
        try {
            return invoker.invoke(arg1, arg2);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }
}
