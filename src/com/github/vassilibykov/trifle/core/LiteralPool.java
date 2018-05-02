// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores objects used by compiled code as literals, which are not
 * directly supported as literals by JVM code.
 */
final class LiteralPool {

    public static final LiteralPool INSTANCE = new LiteralPool();

    private final List<Object> objects = new ArrayList<>();

    private LiteralPool() {}

    public synchronized int register(Object object) {
        int id = objects.size();
        objects.add(object);
        return id;
    }

    public synchronized Object get(int id) {
        try {
            return objects.get(id);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("object ID does not exist: " + id);
        }
    }
}
