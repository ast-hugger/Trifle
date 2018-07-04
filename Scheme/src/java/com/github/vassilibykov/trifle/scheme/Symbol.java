// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Symbol {

    public static Symbol named(String name) {
        return symbolTable.computeIfAbsent(name, Symbol::new);
    }

    // TODO no cleanup right now
    private static final Map<String, Symbol> symbolTable = new ConcurrentHashMap<>();

    /*
        Instance
     */

    private final String name;

    private Symbol(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
