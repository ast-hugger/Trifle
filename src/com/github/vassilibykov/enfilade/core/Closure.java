// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

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
    private final Object[] copiedValues;
    private final MethodHandle invoker;

    Closure(@NotNull FunctionImplementation implementation, @NotNull Object[] copiedValues) {
        this.implementation = implementation;
        this.copiedValues = copiedValues;
        // callSiteInvoker type: (Closure synthetic:Object* declared:Object*) -> Object
        // invoker type: (Closure declared:Object*) -> Object
        this.invoker = MethodHandles.insertArguments(implementation.callSiteInvoker, 1, copiedValues);
    }

    /**
     * A method handle invoking which evaluates the closure. Its type is
     * {@code (Closure Object{n}) -> Object}, where {@code n} is the closure arity.
     */
    MethodHandle genericInvoker() {
        return invoker;
    }

    /**
     * Return an invoker of the requested type, if such an invoker can be created.
     * The result may in fact delegate to the generic invoker after adapting
     * its arguments.
     *
     * @throws IllegalArgumentException if it's impossible to create an invoker of the
     *         requested type, for example because of a function arity mismatch.
     */
    MethodHandle specializedInvoker(MethodType requiredType) {
        if (requiredType.parameterCount() != implementation.declarationArity() + 1) {
            throw new IllegalArgumentException();
        }
        var specializedForm = implementation.specializedImplementation;
        if (specializedForm != null) {
            if (specializedForm.type().equals(requiredType.dropParameterTypes(0, 1))) {
                var invoker = MethodHandles.insertArguments(
                    MethodHandles.dropArguments(specializedForm, 0, Closure.class),
                    1,
                    copiedValues);
                return invoker.asType(requiredType);
            }
        }
        return invoker.asType(requiredType);
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
