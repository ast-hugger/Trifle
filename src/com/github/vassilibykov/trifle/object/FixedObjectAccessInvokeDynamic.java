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
 */
public class FixedObjectAccessInvokeDynamic {

    public static final Handle BOOTSTRAP_GET = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(FixedObjectAccessInvokeDynamic.class),
        "bootstrapGet",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    public static final Handle BOOTSTRAP_SET = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(FixedObjectAccessInvokeDynamic.class),
        "bootstrapSet",
        MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
        false);

    public static String getterName(String fieldName) {
        return "get:" + fieldName;
    }

    public static String setterName(String fieldName) {
        return "set:" + fieldName;
    }

    public static String fieldNameInOperation(String operationName) {
        var index = operationName.indexOf(':');
        if (index < 0) throw new AssertionError();
        return operationName.substring(index + 1);
    }

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrapGet(MethodHandles.Lookup lookup, String operationName, MethodType callSiteType) {
        var fieldName = fieldNameInOperation(operationName);
        var dispatch = DISPATCH_GET.bindTo(fieldName);
        return new InlineCachingCallSite(callSiteType, dispatch); // TODO worry about the megamorphic case later
    }

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrapSet(MethodHandles.Lookup lookup, String operationName, MethodType callSiteType) {
        var fieldName = fieldNameInOperation(operationName);
        var dispatch = DISPATCH_SET.bindTo(fieldName);
        return new InlineCachingCallSite(callSiteType, dispatch); // TODO worry about the megamorphic case later
    }

    public static Object dispatchGet(String fieldName, InlineCachingCallSite thisSite, Object object) throws Throwable {
        if (!(object instanceof FixedObject)) {
            throw RuntimeError.message("not an object: " + object);
        }
        var fixedObject = (FixedObject) object;
        fixedObject.definition().lock();
        try {
            var layout = fixedObject.ensureUpToDateLayout();
            var index = layout.fieldIndex(fieldName);
            if (index < 0) {
                throw RuntimeError.message(String.format("object '%s' has no field '%s", fixedObject, fieldName));
            }
            var getter = MethodHandles.insertArguments(GET, 0, index);
            var handler = layout.switchPoint().guardWithTest(getter, thisSite.resetter());
            thisSite.addCacheEntry(CHECK_LAYOUT.bindTo(layout), handler);
            return handler.invokeExact(object);
        } finally {
            fixedObject.definition().unlock();
        }
    }

    public static void dispatchSet(String fieldName, InlineCachingCallSite thisSite, Object object, Object value)
        throws Throwable
    {
        if (!(object instanceof FixedObject)) {
            throw RuntimeError.message("not an object: " + object);
        }
        var fixedObject = (FixedObject) object;
        fixedObject.definition().lock();
        try {
            var layout = fixedObject.ensureUpToDateLayout();
            var index = layout.fieldIndex(fieldName);
            if (index < 0) {
                throw RuntimeError.message(String.format("object '%s' has no field '%s", fixedObject, fieldName));
            }
            var handler = MethodHandles.insertArguments(SET, 0, index);
            thisSite.addCacheEntry(CHECK_LAYOUT.bindTo(layout), handler);
            handler.invokeExact(object, value);
        } finally {
            fixedObject.definition().unlock();
        }
    }

    public static boolean checkLayout(FixedObjectLayout expected, Object object) {
        return object instanceof FixedObject && ((FixedObject) object).layout == expected;
    }

    public static Object get(int index, Object object) {
        var fixedObject = (FixedObject) object;
        var ref = fixedObject.referenceData[index];
        return ref == FixedObject.NO_VALUE ? fixedObject.intData[index] : ref;
    }

    public static void set(int index, Object object, Object value) {
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
                FixedObjectAccessInvokeDynamic.class,
                "dispatchGet",
                MethodType.methodType(Object.class, String.class, InlineCachingCallSite.class, Object.class));
            DISPATCH_SET = lookup.findStatic(
                FixedObjectAccessInvokeDynamic.class,
                "dispatchSet",
                MethodType.methodType(void.class, String.class, InlineCachingCallSite.class, Object.class, Object.class));
            CHECK_LAYOUT = lookup.findStatic(
                FixedObjectAccessInvokeDynamic.class,
                "checkLayout",
                MethodType.methodType(boolean.class, FixedObjectLayout.class, Object.class));
            GET = lookup.findStatic(
                FixedObjectAccessInvokeDynamic.class,
                "get",
                MethodType.methodType(Object.class, int.class, Object.class));
            SET = lookup.findStatic(
                FixedObjectAccessInvokeDynamic.class,
                "set",
                MethodType.methodType(void.class, int.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
