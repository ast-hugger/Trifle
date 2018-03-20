// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A registry translating between {@link Function} objects and integer IDs.
 */
public class FunctionRegistry {

    public static final FunctionRegistry INSTANCE = new FunctionRegistry();

    private final List<Function> functionsById = new ArrayList<>();
    private final Map<Function, Integer> functionIds = new HashMap<>();

    /**
     * Return the ID associated with the function. The function is added to the
     * registry if it was not registered. The existing ID is returned if it was.
     */
    public synchronized int lookup(Function function) {
        Integer id = functionIds.get(function);
        if (id != null) {
            return id;
        } else {
            int newId = functionsById.size();
            functionsById.add(function);
            functionIds.put(function, newId);
            return newId;
        }
    }

    /**
     * Return the function with the specified ID. Return null if the ID is not
     * mapped to a function.
     */
    @Nullable
    public synchronized Function lookup(int id) {
        try {
            return functionsById.get(id);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
