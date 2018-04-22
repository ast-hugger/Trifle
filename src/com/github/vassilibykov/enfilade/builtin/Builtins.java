// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.builtin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry of known builtins.
 */
public final class Builtins {
    private static final Map<String, BuiltinFunction> builtins = new ConcurrentHashMap<>();

    public static BuiltinFunction lookup(String name) {
        return builtins.get(name);
    }

    static void register(BuiltinFunction builtin) {
        builtins.put(builtin.name(), builtin);
    }
}
