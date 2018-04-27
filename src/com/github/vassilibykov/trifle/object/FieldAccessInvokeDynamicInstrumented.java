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
 * A replica of the default {@link FieldAccessInvokeDynamic} with additional
 * instrumentation to allow inline caching tests.
 */
public class FieldAccessInvokeDynamicInstrumented {

    static FieldAccessImplementation FACTORY = new FieldAccessImplementation() {
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
        GhostWriter.internalClassName(FieldAccessInvokeDynamicInstrumented.class),
        "bootstrapGet",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    private static final Handle BOOTSTRAP_SET = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(FieldAccessInvokeDynamicInstrumented.class),
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

    public static Object dispatchGet(String fieldName, InlineCachingCallSite thisSite, Object object) throws Throwable {
        if (!(object instanceof FixedObject)) {
            throw RuntimeError.message("not an object: " + object);
        }
        var fixedObject = (FixedObject) object;
        var layout = fixedObject.ensureUpToDateLayout();
        var index = layout.fieldIndex(fieldName);
        if (index < 0) {
            throw RuntimeError.message(String.format("object '%s' has no field '%s", fixedObject, fieldName));
        }
        var getter = MethodHandles.insertArguments(GET, 0, index);
        var handler = layout.switchPoint().guardWithTest(getter, thisSite.resetAndDispatchInvoker());
        thisSite.addCacheEntry(CHECK_LAYOUT.bindTo(layout), handler);
        return handler.invokeExact(object);
    }

    public static void dispatchSet(String fieldName, InlineCachingCallSite thisSite, Object object, Object value)
        throws Throwable
    {
        if (!(object instanceof FixedObject)) {
            throw RuntimeError.message("not an object: " + object);
        }
        var fixedObject = (FixedObject) object;
        var layout = fixedObject.ensureUpToDateLayout();
        var index = layout.fieldIndex(fieldName);
        if (index < 0) {
            throw RuntimeError.message(String.format("object '%s' has no field '%s", fixedObject, fieldName));
        }
        var setter = MethodHandles.insertArguments(SET, 0, index);
        var handler = layout.switchPoint().guardWithTest(setter, thisSite.resetAndDispatchInvoker());
        thisSite.addCacheEntry(CHECK_LAYOUT.bindTo(layout), handler);
        handler.invokeExact(object, value);
    }

    private static boolean checkLayout(FixedObjectLayout expected, Object object) {
        return object instanceof FixedObject && ((FixedObject) object).layout == expected;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static Object get(int index, Object object) {
        var fixedObject = (FixedObject) object;
        var ref = fixedObject.referenceData[index];
        return ref == FixedObject.NO_VALUE ? fixedObject.intData[index] : ref;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static void set(int index, Object object, Object value) {
        var fixedObject = (FixedObject) object;
        if (value instanceof Integer) {
            fixedObject.intData[index] = (Integer) value;
            fixedObject.referenceData[index] = FixedObject.NO_VALUE;
        } else {
            fixedObject.referenceData[index] = value;
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
                FieldAccessInvokeDynamicInstrumented.class,
                "dispatchGet",
                MethodType.methodType(Object.class, String.class, InlineCachingCallSite.class, Object.class));
            DISPATCH_SET = lookup.findStatic(
                FieldAccessInvokeDynamicInstrumented.class,
                "dispatchSet",
                MethodType.methodType(void.class, String.class, InlineCachingCallSite.class, Object.class, Object.class));
            CHECK_LAYOUT = lookup.findStatic(
                FieldAccessInvokeDynamicInstrumented.class,
                "checkLayout",
                MethodType.methodType(boolean.class, FixedObjectLayout.class, Object.class));
            GET = lookup.findStatic(
                FieldAccessInvokeDynamicInstrumented.class,
                "get",
                MethodType.methodType(Object.class, int.class, Object.class));
            SET = lookup.findStatic(
                FieldAccessInvokeDynamicInstrumented.class,
                "set",
                MethodType.methodType(void.class, int.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
