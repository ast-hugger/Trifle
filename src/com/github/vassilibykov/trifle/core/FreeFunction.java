// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.lang.invoke.MethodType;

/**
 * A top-level user-defined or built-in function; one with no outer lexical
 * context.
 */
public interface FreeFunction extends Invocable {

    String name();

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
    default Object invoke(Object arg1, Object arg2, Object arg3) {
        var invoker = invoker(MethodType.genericMethodType(3));
        try {
            return invoker.invoke(arg1, arg2, arg3);
        } catch (SquarePegException e) {
            return e.value;
        } catch (RuntimeError e) {
            throw e;
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    @Override
    default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4) {
        var invoker = invoker(MethodType.genericMethodType(4));
        try {
            return invoker.invoke(arg1, arg2, arg3, arg4);
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
        var invoker = invoker(MethodType.genericMethodType(arguments.length));
        try {
            return invoker.invokeWithArguments(arguments);
        } catch (SquarePegException e) {
            return e.value;
        } catch (RuntimeError e) {
            throw e;
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }
}
