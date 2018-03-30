// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

/**
 * An invokedynamic instruction for the usual case of a call expression whose function is
 * a {@link Closure}.
 */
public final class ClosureInvokeDynamic {
    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        Compiler.internalClassName(ClosureInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    public static CallSite bootstrap(Lookup lookupAtCall, String name, MethodType callSiteType) {
        var callSite = new MutableCallSite(callSiteType);
        if (callSiteType.parameterCount() != 2) throw new UnsupportedOperationException(); // FIXME: 3/29/18
        callSite.setTarget(DISPATCH_1.bindTo(callSite));
        return callSite;
    }

    public static Object dispatch(MutableCallSite thisSite, Object expectedClosure, Object arg) throws Throwable {
        var closure = (Closure) expectedClosure;
        var target = closure.invoker.asType(thisSite.type()); // type: (Closure Object*) -> Object
        var guarded = MethodHandles.guardWithTest(CHECK_CLOSURE.bindTo(expectedClosure), target, thisSite.getTarget());
        thisSite.setTarget(guarded);
        return target.invokeExact(expectedClosure, arg);
    }

    public static boolean checkClosure(Object expected, Object actual) {
        return expected == actual;
    }

    private static final MethodHandle CHECK_CLOSURE;
    private static final MethodHandle DISPATCH_1;
    static {
        try {
            var lookup = MethodHandles.lookup();
            CHECK_CLOSURE = lookup.findStatic(
                ClosureInvokeDynamic.class,
                "checkClosure",
                MethodType.methodType(boolean.class, Object.class, Object.class));
            DISPATCH_1 = lookup.findStatic(
                ClosureInvokeDynamic.class,
                "dispatch",
                MethodType.methodType(Object.class, MutableCallSite.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
