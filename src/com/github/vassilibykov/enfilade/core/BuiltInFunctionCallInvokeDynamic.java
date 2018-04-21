// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.builtins.Builtins;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction implementing a call produced by an
 * expression where the call target was a
 * {@link com.github.vassilibykov.enfilade.expression.FreeFunctionReference}
 * to a built-in function.
 */
final class BuiltInFunctionCallInvokeDynamic {
    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(BuiltInFunctionCallInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class)
            .toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrap(MethodHandles.Lookup lookupAtCaller, String name, MethodType callSiteType) {
        var builtin = Builtins.lookup(name);
        var invoker = builtin.invoker(callSiteType);
        return new ConstantCallSite(invoker);
    }
}
