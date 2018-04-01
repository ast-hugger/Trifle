// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction for the usual case of a call expression whose
 * function is a {@link Closure}. For example, in abstract syntax, {@code
 * call(c, a1, a2)}. The call site of the above has the signature of {@code
 * (Object Object Object) -> Object} in the generic case, of {@code (Object
 * <type1> <type2>) -> <type3>} in a specialized case, where {@code <typeN>}
 * is an arbitrary type, reference or primitive.
 */
public final class ClosureInvokeDynamic {
    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        Compiler.internalClassName(ClosureInvokeDynamic.class),
        "bootstrap",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    /**
     * Bootstrap a closure call instruction. The call site in the generic case
     * has the type of {@code (Object Object{n}) -> Object}, where n is the
     * number of call arguments. In the specialized case any of the {@code
     * Object} type occurrences but the first may be replaced with a primitive
     * type.
     *
     * @param lookupAtCall The lookup object with permissions of the caller.
     * @param name The name associated with the call, unused.
     * @param callSiteType The type of the call site as discussed above.
     * @return The call site to use for the instruction.
     */
    public static CallSite bootstrap(Lookup lookupAtCall, String name, MethodType callSiteType) {
        var callArity = callSiteType.parameterCount() - 1; // don't count the closure
        return new InlineCachingCallSite(
            callSiteType,
            site -> DISPATCH.bindTo(site).asCollector(Object[].class, callArity).asType(callSiteType));
    }

    public static Object dispatch(InlineCachingCallSite thisSite, Object closureArg, Object[] args) throws Throwable {
        var closure = (Closure) closureArg;
        if (!thisSite.isMegamorphic()) {
            var target = closure.specializedInvoker(thisSite.type());
            thisSite.addCacheEntry(IS_SAME_FUNCTION.bindTo(closure.implementation), target);
        }
        switch (args.length) {
            case 0:
                return closure.genericInvoker().invokeExact(closure);
            case 1:
                return closure.genericInvoker().invokeExact(closure, args[0]);
            case 2:
                return closure.genericInvoker().invokeExact(closure, args[0], args[1]);
            case 3:
                return closure.genericInvoker().invokeExact(closure, args[0], args[1], args[2]);
            case 4:
                return closure.genericInvoker().invokeExact(closure, args[0], args[1], args[2], args[3]);
            default:
                var combined = new Object[args.length + 1];
                combined[0] = closure;
                System.arraycopy(args, 0, combined, 1, args.length);
                return closure.genericInvoker().invokeWithArguments(combined);
        }
    }

    public static boolean isSameFunction(FunctionImplementation expected, Object closureArg) {
        return closureArg instanceof Closure && ((Closure) closureArg).implementation == expected;
    }

    private static final MethodHandle IS_SAME_FUNCTION;
    private static final MethodHandle DISPATCH;
    static {
        try {
            var lookup = MethodHandles.lookup();
            IS_SAME_FUNCTION = lookup.findStatic(
                ClosureInvokeDynamic.class,
                "isSameFunction",
                MethodType.methodType(boolean.class, FunctionImplementation.class, Object.class));
            DISPATCH = lookup.findStatic(
                ClosureInvokeDynamic.class,
                "dispatch",
                MethodType.methodType(Object.class, InlineCachingCallSite.class, Object.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
