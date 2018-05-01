// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.builtin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A registry of known builtins.
 */
public final class Builtins {
    private static final Map<String, BuiltinFunction> builtins = new ConcurrentHashMap<>();

    public static void register(BuiltinFunction builtin) {
        builtins.put(builtin.name(), builtin);
    }

    public static BuiltinFunction lookup(String name) {
        return builtins.get(name);
    }

    public static Stream<BuiltinFunction> builtins() {
        return builtins.values().stream();
    }
}
