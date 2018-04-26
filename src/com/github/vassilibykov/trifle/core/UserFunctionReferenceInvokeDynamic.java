// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.expression.FreeFunctionReference;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

/**
 * An invokedynamic instruction implementing a {@link FreeFunctionReference} to
 * a user function used as a standalone expression (not in the call target
 * position of a call expression). Such an expression evaluates to an {@link
 * Invocable} which invokes the function.
 */
public class UserFunctionReferenceInvokeDynamic {
    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(UserFunctionReferenceInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Integer.class)
            .toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrap(MethodHandles.Lookup lookupAtCaller, String name, MethodType callSiteType, Integer functionId) {
        var function = Objects.requireNonNull(
            FunctionImplementation.withId(functionId).userFunction(),
            "sanity check failure: user function for this id is not set");
        return new ConstantCallSite(MethodHandles.constant(Object.class, function));
    }
}
