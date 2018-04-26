// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction for a closure creation site. The closure's
 * copied values are stacked prior to invoking the instruction. The function ID
 * identifying the closure function is encoded as an additional instruction
 * parameter. The call site permanently links to
 * {@link Closure#create(FunctionImplementation, Object[])}, with the function
 * implementation parameter bound to the proper function.
 */
final class ClosureCreationInvokeDynamic {
    static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(ClosureCreationInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Integer.class).toMethodDescriptorString(),
        false);

    private static final MethodHandle CREATE_CLOSURE;
    static {
        try {
            CREATE_CLOSURE = MethodHandles.lookup().findStatic(
                Closure.class,
                "create",
                MethodType.methodType(Closure.class, FunctionImplementation.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrap(MethodHandles.Lookup lookupAtCaller, String name, MethodType callSiteType, Integer targetId) {
        var handler = CREATE_CLOSURE
            .bindTo(FunctionImplementation.withId(targetId))
            .asCollector(Object[].class, callSiteType.parameterCount())
            .asType(callSiteType);
        return new ConstantCallSite(handler);
    }

}
