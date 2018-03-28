// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains mapping between {@link Lambda}s and {@link FunctionImplementation}s they have been
 * translated to, as well as between function IDs and functions.
 */
public class Environment {
    public static final Environment INSTANCE = new Environment();

    /*
        Instance
     */

    private final List<FunctionImplementation> functionsById = new ArrayList<>();
    private final Map<Lambda, FunctionImplementation> functionsByDefinition = new HashMap<>();

    private Environment() {}

    public void compile(List<Lambda> functions) {
        functions.forEach(FunctionTranslator::translate);
    }

    public synchronized FunctionImplementation lookup(Lambda source) {
        return functionsByDefinition.get(source);
    }

    public synchronized FunctionImplementation lookupOrMake(Lambda source) {
        return functionsByDefinition.computeIfAbsent(source, k -> new FunctionImplementation(source));
    }

    /**
     * Return the ID associated with the function, adding the function to the registry as
     * needed.
     */
    public synchronized int lookup(FunctionImplementation function) {
        Integer id = function.id();
        if (id >= 0) {
            return id;
        } else {
            int newId = functionsById.size();
            function.setId(newId);
            functionsById.add(function);
            return newId;
        }
    }

    /**
     * Return the function with the specified ID. Return null if the ID is not mapped to any
     * function.
     */
    @Nullable
    public synchronized FunctionImplementation lookup(int id) {
        try {
            return functionsById.get(id);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

}
