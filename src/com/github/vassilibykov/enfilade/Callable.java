// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade;

import com.github.vassilibykov.enfilade.core.InvocationException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public interface Callable {
    int id();
    MethodHandle invoker(MethodType callSiteType);

    default Object call() {
        var invoker = invoker(MethodType.genericMethodType(0));
        try {
            return invoker.invoke();
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    default Object call(Object arg) {
        var invoker = invoker(MethodType.genericMethodType(1));
        try {
            return invoker.invoke(arg);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    default Object call(Object arg1, Object arg2) {
        var invoker = invoker(MethodType.genericMethodType(2));
        try {
            return invoker.invoke(arg1, arg2);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }
}
