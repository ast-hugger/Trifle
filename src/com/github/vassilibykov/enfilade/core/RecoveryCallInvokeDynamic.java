// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * An invokedynamic instruction for the call of a recovery method
 * from and SPE handler.
 */
class RecoveryCallInvokeDynamic {

    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        Compiler.internalClassName(RecoveryCallInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Integer.class)
            .toMethodDescriptorString(),
        false);

    private static final MethodHandle SPE_EXTRACTOR;
    static {
        try {
            VarHandle speValue = MethodHandles.lookup().findVarHandle(SquarePegException.class, "value", Object.class);
            SPE_EXTRACTOR = speValue.toMethodHandle(VarHandle.AccessMode.GET);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * The call site type is always {@code (SPE, int, Object[]) -> Object}.
     */
    @SuppressWarnings("unused") // called by generated code
    public static CallSite bootstrap(
            MethodHandles.Lookup lookupAtCaller,
            String name,
            MethodType callSiteType,
            Integer targetId)
    {
        var function = Objects.requireNonNull(FunctionRegistry.INSTANCE.lookup(targetId),
            "internal error: function ID not found");
        var recoveryMethod = Objects.requireNonNull(function.recoveryImplementation,
            "internal error: function has no recovery method");
        var handler = MethodHandles.filterArguments(recoveryMethod, 0, SPE_EXTRACTOR);
        return new ConstantCallSite(handler);
    }
}
