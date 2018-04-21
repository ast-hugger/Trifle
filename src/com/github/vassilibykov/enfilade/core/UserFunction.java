// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Function;

/**
 * A user-defined function created and managed by {@link TopLevel}.
 */
public class UserFunction implements FreeFunction {
    static UserFunction construct(String name, Function<UserFunction, FunctionImplementation> implementationMaker) {
        return new UserFunction(name, implementationMaker);
    }

    /*
        Instance
     */

    @NotNull private final String name;
    @NotNull private final FunctionImplementation implementation;

    private UserFunction(@NotNull String name, Function<UserFunction, FunctionImplementation> implementationMaker) {
        this.name = name;
        this.implementation = Objects.requireNonNull(implementationMaker.apply(this));
        this.implementation.setUserFunction(this);
    }

    public String name() {
        return name;
    }

    FunctionImplementation implementation() {
        return implementation;
    }

    @Override
    public MethodHandle invoker(MethodType callSiteType) {
        return implementation.callSiteInvoker().asType(callSiteType);
    }
}
