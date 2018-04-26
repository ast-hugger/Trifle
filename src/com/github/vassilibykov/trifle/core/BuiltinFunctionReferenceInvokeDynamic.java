// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.builtin.Builtins;
import com.github.vassilibykov.trifle.expression.FreeFunctionReference;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction implementing a {@link FreeFunctionReference} to
 * a built-in function used as a standalone expression (not in the call target
 * position of a call expression). Such an expression evaluates to an {@link
 * Invocable} which invokes the function.
 */
public class BuiltinFunctionReferenceInvokeDynamic {
    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(BuiltinFunctionReferenceInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class)
            .toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrap(MethodHandles.Lookup lookupAtCaller, String name, MethodType callSiteType) {
        var builtin = Builtins.lookup(name);
        return new ConstantCallSite(MethodHandles.constant(Object.class, builtin));
    }
}
