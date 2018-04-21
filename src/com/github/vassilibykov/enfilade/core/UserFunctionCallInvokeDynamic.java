// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.FreeFunctionReference;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction for a call expression whose function {@link FreeFunctionReference}
 * to a user function. The call target is identified by an integer ID
 * attached to the instruction as an extra parameter. The call target is nominally a closure,
 * however the whole mechanism is intended for top-level functions, so it is a closure which
 * closes over nothing and therefore the implementation function's parameter list is identical
 * to the declaration parameter list.
 *
 * <p>The call site of such an instruction has no leading closure parameter.
 */
public final class UserFunctionCallInvokeDynamic {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(UserFunctionCallInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Integer.class).toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrap(Lookup lookupAtCaller, String name, MethodType callSiteType, Integer targetId) {
        var callable = FunctionImplementation.withId(targetId);
        return new ConstantCallSite(callable.invoker(callSiteType));
    }
}
