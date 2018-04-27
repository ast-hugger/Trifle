// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.lang.invoke.MethodType;

/**
 * A top-level user-defined or built-in function; one with no outer lexical
 * context.
 */
public interface FreeFunction extends Invocable {

    @Override
    default Object invoke() {
        var invoker = invoker(MethodType.genericMethodType(0));
        try {
            return invoker.invoke();
        } catch (SquarePegException e) {
            return e.value;
        } catch (RuntimeError e) {
            throw e;
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    @Override
    default Object invoke(Object arg) {
        var invoker = invoker(MethodType.genericMethodType(1));
        try {
            return invoker.invoke(arg);
        } catch (SquarePegException e) {
            return e.value;
        } catch (RuntimeError e) {
            throw e;
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    @Override
    default Object invoke(Object arg1, Object arg2) {
        var invoker = invoker(MethodType.genericMethodType(2));
        try {
            return invoker.invoke(arg1, arg2);
        } catch (SquarePegException e) {
            return e.value;
        } catch (RuntimeError e) {
            throw e;
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    @Override
    default Object invokeWithArguments(Object[] arguments) {
        switch (arguments.length) {
            case 0:
                return invoke();
            case 1:
                return invoke(arguments[0]);
            case 2:
                return invoke(arguments[0], arguments[1]);
            default:
                throw new UnsupportedOperationException("not supported yet");
        }
    }
}
