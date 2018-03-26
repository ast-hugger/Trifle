// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction for a call expression whose function is a direct
 * pointer to another function. The target is encoded as an integer ID in the
 * {@link Environment}, passed as an extra bootstrapper argument.
 */
public class DirectCall {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        Compiler.internalClassName(DirectCall.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Integer.class).toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by generated code
    public static CallSite bootstrap(Lookup lookupAtCaller, String name, MethodType callSiteType, Integer targetId) {
        RuntimeFunction target = Environment.INSTANCE.lookup(targetId);
        if (target == null) {
            throw new AssertionError("target function ID not found: " + targetId);
        }
        return target.callSite(callSiteType);
    }
}
