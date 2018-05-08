// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.NoSuchElementException;

/**
 * An invokedynamic instruction for getting or setting a value in a {@link Dictionary}.
 */
public class DictionaryAccessInvokeDynamic {

    public static final Handle BOOTSTRAP_GET = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(DictionaryAccessInvokeDynamic.class),
        "bootstrapGet",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Integer.class)
            .toMethodDescriptorString(),
        false);

    public static final Handle BOOTSTRAP_SET = new Handle(
        Opcodes.H_INVOKESTATIC,
        GhostWriter.internalClassName(DictionaryAccessInvokeDynamic.class),
        "bootstrapSet",
        MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Integer.class)
            .toMethodDescriptorString(),
        false);

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrapGet(Lookup lookup, String operation, MethodType callSiteType, Integer id) {
        var dictionary = Dictionary.withId(id);
        String name = keyIn(operation);
        var entry = dictionary.getEntry(name).orElseThrow(NoSuchElementException::new); // TODO use a proper exception
        var handle = callSiteType.returnType() == int.class ? GET_INT : GET_REF;
        return new ConstantCallSite(handle.bindTo(entry));
    }

    @SuppressWarnings("unused") // called by invokedynamic infrastructure
    public static CallSite bootstrapSet(Lookup lookup, String operation, MethodType callSiteType, Integer id) {
        var dictionary = Dictionary.withId(id);
        var entry = dictionary.getEntry(keyIn(operation)).orElseThrow(NoSuchElementException::new); // TODO use a proper exception
        var handle = callSiteType.parameterType(0) == int.class ? GET_INT : GET_REF;
        return new ConstantCallSite(handle.bindTo(entry));
    }

    static String getterName(String key) {
        return "get:" + key;
    }

    static String setterName(String key) {
        return "set:" + key;
    }

    private static String keyIn(String operation) {
        var index = operation.indexOf(":");
        if (index < 0) throw new AssertionError();
        return operation.substring(index + 1);
    }

    private static final MethodHandle GET_INT;
    private static final MethodHandle GET_REF;
    private static final MethodHandle SET_INT;
    private static final MethodHandle SET_REF;

    static {
        try {
            var lookup = MethodHandles.lookup();
            GET_INT = lookup.findVirtual(Dictionary.Entry.class, "intValue", MethodType.methodType(int.class));
            GET_REF = lookup.findVirtual(Dictionary.Entry.class, "value", MethodType.methodType(Object.class));
            SET_INT = lookup.findVirtual(Dictionary.Entry.class, "setValue", MethodType.methodType(void.class, int.class));
            SET_REF = lookup.findVirtual(Dictionary.Entry.class, "setValue", MethodType.methodType(void.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError();
        }
    }
}
