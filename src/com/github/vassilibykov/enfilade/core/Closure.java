// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

/**
 * A function value in a program. Produced by evaluating a lambda expression or
 * by referencing a top-level function.
 */
public class Closure {
    @SuppressWarnings("unused") // called by generated code
    public static Closure create(Object[] copiedValues, int functionId) {
        return new Closure(
            Objects.requireNonNull(FunctionRegistry.INSTANCE.lookup(functionId)),
            copiedValues);
    }

    @NotNull /*internal*/ final FunctionImplementation implementation;
    private final boolean hasNoCopiedValues;
    /*internal*/ final MethodHandle invoker;

    Closure(@NotNull FunctionImplementation implementation, @NotNull Object[] copiedValues) {
        this.implementation = implementation;
        this.hasNoCopiedValues = copiedValues.length == 0;
        // callSiteInvoker type: (Closure synthetic:Object* declared:Object*) -> Object
        // invoker type: (Closure declared:Object*) -> Object
        this.invoker = MethodHandles.insertArguments(implementation.callSiteInvoker, 1, copiedValues);
    }

    /**
     * Return a call site of a signature without the leading closure argument.
     * Used by constant function call sites.
     */
    /*internal*/ CallSite directCallSite(MethodType requiredType) {
        if (hasNoCopiedValues) {
            return implementation.callSite(requiredType);
        } else {
            // Constant top-level functions never have copied values.
            throw new UnsupportedOperationException("not implemented yet"); // TODO implement
        }
    }

    public Object invoke() {
        try {
            return invoker.invokeExact(this);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    public Object invoke(Object arg) {
        try {
            return invoker.invokeExact(this, arg);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    public Object invoke(Object arg1, Object arg2) {
        try {
            return invoker.invokeExact(this, arg1, arg2);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    public Object invoke(Object... args) {
        switch (args.length) {
            case 0:
                return invoke();
            case 1:
                return invoke(args[0]);
            case 2:
                return invoke(args[0], args[1]);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
