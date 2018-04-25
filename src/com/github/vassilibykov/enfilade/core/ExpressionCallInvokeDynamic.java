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
 *
 * <p>See {@code doc/closure-calls.md} for a design overview of closure invocations.
 */
final class ExpressionCallInvokeDynamic {
    public static final Handle BOOTSTRAP = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(ExpressionCallInvokeDynamic.class),
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
    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrap(Lookup lookupAtCall, String name, MethodType callSiteType) {
        var callArity = callSiteType.parameterCount() - 1; // don't count the closure
        return new InlineCachingCallSite(
            callSiteType,
            DISPATCH.asCollector(Object[].class, callArity),
            MEGAMORPHIC_DISPATCH.asCollector(Object[].class, callArity));
    }

    public static Object dispatch(InlineCachingCallSite thisSite, Object closureArg, Object[] args) throws Throwable {
        if (closureArg instanceof FreeFunction) {
            return dispatchFreeFunctionCall(thisSite, (FreeFunction) closureArg, args);
        }
        var closure = (Closure) closureArg;
        MethodHandle target;
        /* If the implementation function has not been compiled, it doesn't
           make sense to install an inline cache just yet because we can do better
           later. */
        if (closure.implementation.isCompiled() && !closure.hasCopiedValues() && !thisSite.isMegamorphic()) {
            // TODO I think we can still do something for closures with copied values, but with a different logic
            target = closure.optimalCallSiteInvoker(thisSite.type());
            thisSite.addCacheEntry(
                IS_SAME_FUNCTION_CLOSURE.bindTo(closure.implementation),
                MethodHandles.dropArguments(target, 0, Object.class));
            // don't use the cached invoker (target) in this call; here we need a generic signature
        }
        target = closure.genericInvoker();
        Object result;
        switch (args.length) {
            case 0:
                result = target.invokeExact();
                break;
            case 1:
                result = target.invokeExact(args[0]);
                break;
            case 2:
                result = target.invokeExact(args[0], args[1]);
                break;
            case 3:
                result = target.invokeExact(args[0], args[1], args[2]);
                break;
            case 4:
                result = target.invokeExact(args[0], args[1], args[2], args[3]);
                break;
            default:
                result = target.invokeWithArguments(args);
        }
        if (JvmType.isCompatibleValue(thisSite.type().returnType(), result)) {
            return result;
        } else {
            throw SquarePegException.with(result);
        }
    }

    private static Object dispatchFreeFunctionCall(InlineCachingCallSite thisSite, FreeFunction function, Object[] args) throws Throwable {
        var adjustedType = thisSite.type().dropParameterTypes(0, 1);
        var realInvoker = function.invoker(adjustedType);
        var adjustedInvoker = MethodHandles.dropArguments(realInvoker, 0, Object.class);
        thisSite.addCacheEntry(IS_SAME_OBJECT.bindTo(function), adjustedInvoker);
        var result = realInvoker.invokeWithArguments(args);
        if (JvmType.isCompatibleValue(thisSite.type().returnType(), result)) {
            return result;
        } else {
            throw SquarePegException.with(result);
        }
    }

    /**
     * Called by a call site in the megamorphic state.
     */
    public static Object megamorphicDispatch(InlineCachingCallSite thisSite, Object closureArg, Object[] args)
        throws Throwable
    {
        Object result;
        if (closureArg instanceof FreeFunction) {
            var adjustedType = thisSite.type().dropParameterTypes(0, 1);
            result = ((FreeFunction) closureArg).invoker(adjustedType).invokeWithArguments(args);
        } else {
            MethodHandle target = ((Closure) closureArg).genericInvoker();
            switch (args.length) {
                case 0:
                    result = target.invokeExact();
                    break;
                case 1:
                    result = target.invokeExact(args[0]);
                    break;
                case 2:
                    result = target.invokeExact(args[0], args[1]);
                    break;
                case 3:
                    result = target.invokeExact(args[0], args[1], args[2]);
                    break;
                case 4:
                    result = target.invokeExact(args[0], args[1], args[2], args[3]);
                    break;
                default:
                    result = target.invokeWithArguments(args);
            }
        }
        if (JvmType.isCompatibleValue(thisSite.type().returnType(), result)) {
            return result;
        } else {
            throw SquarePegException.with(result);
        }
    }

    public static boolean isClosureOfSameFunction(FunctionImplementation expected, Object closureArg) {
        return closureArg instanceof Closure && ((Closure) closureArg).implementation == expected;
    }

    public static boolean isSameObject(Object expected, Object actual) {
        return expected == actual;
    }

    private static final MethodHandle IS_SAME_FUNCTION_CLOSURE;
    private static final MethodHandle IS_SAME_OBJECT;
    private static final MethodHandle DISPATCH;
    private static final MethodHandle MEGAMORPHIC_DISPATCH;
    static {
        try {
            var lookup = MethodHandles.lookup();
            IS_SAME_FUNCTION_CLOSURE = lookup.findStatic(
                ExpressionCallInvokeDynamic.class,
                "isClosureOfSameFunction",
                MethodType.methodType(boolean.class, FunctionImplementation.class, Object.class));
            IS_SAME_OBJECT = lookup.findStatic(
                ExpressionCallInvokeDynamic.class,
                "isSameObject",
                MethodType.methodType(boolean.class, Object.class, Object.class));
            DISPATCH = lookup.findStatic(
                ExpressionCallInvokeDynamic.class,
                "dispatch",
                MethodType.methodType(Object.class, InlineCachingCallSite.class, Object.class, Object[].class));
            MEGAMORPHIC_DISPATCH = lookup.findStatic(
                ExpressionCallInvokeDynamic.class,
                "megamorphicDispatch",
                MethodType.methodType(Object.class, InlineCachingCallSite.class, Object.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
