// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * A loader and container of generated code. Using Unsafe to load generated
 * code, as opposed to using a custom classloader as in early versions, appears
 * to result in slightly better performance for specialized code.
 */
@SuppressWarnings("sunapi")
class GeneratedCode {
    private static final Unsafe UNSAFE = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            return (Unsafe) singleoneInstanceField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    static Class<?> defineClass(Compiler.BatchResult compilerBatchResult) {
        return UNSAFE.defineAnonymousClass(GeneratedCode.class, compilerBatchResult.bytecode(), null);
    }
}
