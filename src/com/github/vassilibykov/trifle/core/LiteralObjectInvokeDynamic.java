// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction for the sites where a literal object not
 * directly supported by JVM code is loaded on the stack. The object is found by
 * its ID encoded in the instruction. The object is registered with and the ID
 * is obtained from {@link LiteralPool}.
 */
final class LiteralObjectInvokeDynamic {

    static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(LiteralObjectInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Integer.class)
            .toMethodDescriptorString(),
        false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType callSiteType, Integer objectId) {
        var object = LiteralPool.INSTANCE.get(objectId);
        return new ConstantCallSite(MethodHandles.constant(callSiteType.returnType(), object));
    }
}
