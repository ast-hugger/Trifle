// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction for a call expression whose function is a constant managed
 * by {@link ConstantFunctionNode}. The call target is identified by an integer ID
 * attached to the instruction as an extra parameter. The call target is nominally a closure,
 * however the whole mechanism is intended for top-level functions, so it is a closure which
 * closes over nothing and therefore the implementation function's parameter list is identical
 * to the declaration parameter list..
 *
 * <p>The call site of such an instruction has no leading closure parameter.
 */
public final class ConstantFunctionInvokeDynamic {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        Compiler.internalClassName(ConstantFunctionInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Integer.class).toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by generated code
    public static CallSite bootstrap(Lookup lookupAtCaller, String name, MethodType callSiteType, Integer targetId) {
        var closure = ConstantFunctionNode.lookup(targetId);
        // FIXME: 3/31/18 the following .asType() introduces expensive boxing; should handle specialized cases without it
        return closure.directCallSite(callSiteType);
    }
}
