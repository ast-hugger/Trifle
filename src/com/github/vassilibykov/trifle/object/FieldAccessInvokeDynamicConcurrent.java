// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.InlineCachingCallSite;
import com.github.vassilibykov.trifle.core.RuntimeError;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An invokedynamic instruction of a call site getting or setting the value of a
 * field of a {@link FixedObject}.
 *
 * <p>This implementation fully synchronizes (or at least intends to) access
 * to data objects, so concurrent access to a data object from multiple threads
 * should work as intended. This is probably an overkill, and the default
 * implementation with thread confinement assumption is sufficient in practice.
 */
public class FieldAccessInvokeDynamicConcurrent {

    public static FieldAccessImplementation FACTORY = new FieldAccessImplementation() {
        @Override
        public Handle getterBootstrapper() {
            return BOOTSTRAP_GET;
        }

        @Override
        public Handle setterBootstrapper() {
            return BOOTSTRAP_SET;
        }
    };


    private static final Handle BOOTSTRAP_GET = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(FieldAccessInvokeDynamicConcurrent.class),
        "bootstrapGet",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    private static final Handle BOOTSTRAP_SET = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(FieldAccessInvokeDynamicConcurrent.class),
        "bootstrapSet",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrapGet(MethodHandles.Lookup lookup, String operationName, MethodType callSiteType) {
        var fieldName = FieldAccessImplementation.extractFieldName(operationName);
        var dispatch = DISPATCH_GET.bindTo(fieldName);
        return new InlineCachingCallSite(callSiteType, dispatch); // TODO worry about the megamorphic case later
    }

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrapSet(MethodHandles.Lookup lookup, String operationName, MethodType callSiteType) {
        var fieldName = FieldAccessImplementation.extractFieldName(operationName);
        var dispatch = DISPATCH_SET.bindTo(fieldName);
        return new InlineCachingCallSite(callSiteType, dispatch); // TODO worry about the megamorphic case later
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static Object dispatchGet(String fieldName, InlineCachingCallSite thisSite, Object object) throws Throwable {
        if (!(object instanceof FixedObject)) {
            throw RuntimeError.message("not an object: " + object);
        }
        var fixedObject = (FixedObject) object;
        synchronized (fixedObject) {
            fixedObject.definition().lock();
            try {
                var layout = fixedObject.ensureUpToDateLayout();
                var index = layout.fieldIndex(fieldName);
                if (index < 0) {
                    throw RuntimeError.message(String.format("object '%s' has no field '%s", fixedObject, fieldName));
                }
                var getter = MethodHandles.insertArguments(GET, 0, layout, index);
                getter = MethodHandles.catchException( // see the block comment below
                    getter,
                    StaleLayoutException.class,
                    MethodHandles.dropArguments(thisSite.resetAndDispatchInvoker(), 0, StaleLayoutException.class));
                var handler = layout.switchPoint().guardWithTest(getter, thisSite.resetAndDispatchInvoker());
                thisSite.addCacheEntry(CHECK_LAYOUT.bindTo(layout), handler);
                return handler.invokeExact(object);
            } finally {
                fixedObject.definition().unlock();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void dispatchSet(String fieldName, InlineCachingCallSite thisSite, Object object, Object value)
        throws Throwable
    {
        if (!(object instanceof FixedObject)) {
            throw RuntimeError.message("not an object: " + object);
        }
        var fixedObject = (FixedObject) object;
        synchronized (fixedObject) {
            fixedObject.definition().lock();
            try {
                var layout = fixedObject.ensureUpToDateLayout();
                var index = layout.fieldIndex(fieldName);
                if (index < 0) {
                    throw RuntimeError.message(String.format("object '%s' has no field '%s", fixedObject, fieldName));
                }
                var setter = MethodHandles.insertArguments(SET, 0, layout, index);
                setter = MethodHandles.catchException( // see the block comment below
                    setter,
                    StaleLayoutException.class,
                    MethodHandles.dropArguments(thisSite.resetAndDispatchInvoker(), 0, StaleLayoutException.class));
                var handler = layout.switchPoint().guardWithTest(setter, thisSite.resetAndDispatchInvoker());
                thisSite.addCacheEntry(CHECK_LAYOUT.bindTo(layout), handler);
                handler.invokeExact(object, value);
            } finally {
                fixedObject.definition().unlock();
            }
        }
    }

    /*
        The following 'checkLayout' test method is combined with the 'get' and
        'set' action methods using 'guardWithTest' to create an inline cache
        entry. I don't see an easy way to compose the methods to properly lock
        the object before the test and unlock it on test failure or after the
        guarded action. Thus, there is a chance of the object's layout changing
        between the test and the action. Here is how we deal with this issue.

        The 'layout' field is volatile and can thus be checked by 'checkLayout'
        with no extra precautions. Additionally, the cache action is also bound
        to the layout it expects to work with. The action locks the object and
        then checks the layout a second time to ensure it's still what's
        expected. Almost always this test succeeds. In the rare case when it
        fails, the action throws an exception. The exception causes the call
        site to be reset, the same way it's reset by an invalidation of a layout
        switch point.
     */

    public static boolean checkLayout(FixedObjectLayout expected, Object object) {
        return object instanceof FixedObject && ((FixedObject) object).layout == expected;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static Object get(FixedObjectLayout expectedLayout, int index, Object object) {
        synchronized (object) {
            var fixedObject = (FixedObject) object;
            if (fixedObject.layout != expectedLayout) throw new StaleLayoutException();
            var ref = fixedObject.referenceData[index];
            return ref == FixedObject.NO_VALUE ? fixedObject.intData[index] : ref;
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void set(FixedObjectLayout expectedLayout, int index, Object object, Object value) {
        synchronized (object) {
            var fixedObject = (FixedObject) object;
            if (fixedObject.layout != expectedLayout) throw new StaleLayoutException();
            if (value instanceof Integer) {
                fixedObject.intData[index] = (Integer) value;
                fixedObject.referenceData[index] = FixedObject.NO_VALUE;
            } else {
                fixedObject.referenceData[index] = value;
            }
        }
    }

    private static final MethodHandle DISPATCH_GET;
    private static final MethodHandle DISPATCH_SET;
    private static final MethodHandle CHECK_LAYOUT;
    private static final MethodHandle GET;
    private static final MethodHandle SET;
    static {
        try {
            var lookup = MethodHandles.lookup();
            DISPATCH_GET = lookup.findStatic(
                FieldAccessInvokeDynamicConcurrent.class,
                "dispatchGet",
                MethodType.methodType(Object.class, String.class, InlineCachingCallSite.class, Object.class));
            DISPATCH_SET = lookup.findStatic(
                FieldAccessInvokeDynamicConcurrent.class,
                "dispatchSet",
                MethodType.methodType(void.class, String.class, InlineCachingCallSite.class, Object.class, Object.class));
            CHECK_LAYOUT = lookup.findStatic(
                FieldAccessInvokeDynamicConcurrent.class,
                "checkLayout",
                MethodType.methodType(boolean.class, FixedObjectLayout.class, Object.class));
            GET = lookup.findStatic(
                FieldAccessInvokeDynamicConcurrent.class,
                "get",
                MethodType.methodType(Object.class, FixedObjectLayout.class, int.class, Object.class));
            SET = lookup.findStatic(
                FieldAccessInvokeDynamicConcurrent.class,
                "set",
                MethodType.methodType(void.class, FixedObjectLayout.class, int.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    static class StaleLayoutException extends RuntimeException {
    }
}
