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
    private final MethodHandle genericInvoker;

    Closure(@NotNull FunctionImplementation implementation, @NotNull Object[] copiedValues) {
        this.implementation = implementation;
        this.copiedValues = copiedValues;
        // callSiteInvoker type: (synthetic:Object* declared:Object*) -> Object
        // invoker type: (declared:Object*) -> Object
        this.genericInvoker = MethodHandles.insertArguments(implementation.callSiteInvoker, 0, copiedValues);
    }

    /**
     * A method handle invoking which evaluates the closure. Its type is
     * {@code (Object{n}) -> Object}, where {@code n} is the closure arity.
     */
    MethodHandle genericInvoker() {
        return genericInvoker;
    }

    /**
     * Return an invoker of the specified type, where the type only considers
     * the function's declared parameters. It does not include the leading
     * closure as do the types of invokedynamic call sites. The result uses
     * the implementation's specialized compiled form, if possible.
     */
    MethodHandle specializedInvoker(MethodType requiredType) {
        if (requiredType.parameterCount() != implementation.declarationArity()) {
            throw new IllegalArgumentException();
        }
        var specializedForm = implementation.specializedImplementation;
        if (specializedForm != null) {
            // The type of specializedForm includes the leading parameters for copied values
            var cleanType = specializedForm.type().dropParameterTypes(0, copiedValues.length);
            if (cleanType.equals(requiredType)) {
                var specializedInvoker = MethodHandles.insertArguments(specializedForm, 0, copiedValues);
                return specializedInvoker.asType(requiredType);
            }
        }
        return genericInvoker.asType(requiredType);
    }

    /**
     * Return an invoker of the requested type. The type describes the call site
     * which will be bound to the invoker, so it has the additional leading
     * {@code Object} parameter to receive the closure.
     *
     * @throws IllegalArgumentException if it's impossible to create an invoker of the
     *         requested type, for example because of a function arity mismatch.
     */
    MethodHandle invokerForCallSite(MethodType callSiteType) {
        var typeWithoutLeadingClosure = callSiteType.dropParameterTypes(0, 1);
        return specializedInvoker(typeWithoutLeadingClosure);
    }

    public Object invoke() {
        try {
            return genericInvoker.invokeExact();
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    public Object invoke(Object arg) {
        try {
            return genericInvoker.invokeExact(arg);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    public Object invoke(Object arg1, Object arg2) {
        try {
            return genericInvoker.invokeExact(arg1, arg2);
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
