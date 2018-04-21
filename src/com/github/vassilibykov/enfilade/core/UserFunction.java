// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Function;

/**
 * A user-defined function created and managed by {@link TopLevel}.
 */
public class UserFunction implements FreeFunction {
    static UserFunction construct(Function<UserFunction, FunctionImplementation> implementationMaker) {
        return new UserFunction(implementationMaker);
    }

    /*
        Instance
     */

    private final FunctionImplementation implementation;

    private UserFunction(Function<UserFunction, FunctionImplementation> implementationMaker) {
        this.implementation = implementationMaker.apply(this);
        this.implementation.setUserFunction(this);
    }

    FunctionImplementation implementation() {
        return implementation;
    }

    @Override
    public MethodHandle invoker(MethodType callSiteType) {
        return implementation.callSiteInvoker().asType(callSiteType);
    }
}
