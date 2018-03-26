// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Function;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A registry translating between {@link RuntimeFunction} objects and integer IDs.
 */
public class Environment {
    public static final Environment INSTANCE = new Environment();

    private final List<RuntimeFunction> functionsById = new ArrayList<>();
    private final Map<RuntimeFunction, Integer> functionIds = new HashMap<>();
    private final Map<com.github.vassilibykov.enfilade.expression.Function, RuntimeFunction> functionsBySource = new HashMap<>();

    public void compile(List<Function> functions) {
        functions.forEach(FunctionTranslator::translate);
    }

    public synchronized RuntimeFunction lookup(Function source) {
        return functionsBySource.computeIfAbsent(source, k -> {
            throw new CompilerError("function not found: " + source);
        });
    }

    public synchronized RuntimeFunction lookupOrMake(com.github.vassilibykov.enfilade.expression.Function source) {
        return functionsBySource.computeIfAbsent(source, k -> new RuntimeFunction(source));
    }

    /**
     * Return the ID associated with the function. The function is added to the
     * registry if it was not registered. The existing ID is returned if it was.
     */
    public synchronized int lookup(RuntimeFunction function) {
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
    public synchronized RuntimeFunction lookup(int id) {
        try {
            return functionsById.get(id);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

}
