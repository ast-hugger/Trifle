// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maintains mapping between {@link Lambda}s and {@link FunctionImplementation}s they have been
 * translated to, as well as between function IDs and functions.
 */
public class FunctionRegistry {
    public static final FunctionRegistry INSTANCE = new FunctionRegistry();

    @SuppressWarnings("unused") // called by generated code
    public static Closure findAsClosure(int functionId) {
        var function = Objects.requireNonNull(INSTANCE.lookup(functionId));
        if (!function.isTopLevel()) throw new AssertionError();
        return new Closure(function, new Object[0]);
    }

    /*
        Instance
     */

    private final List<FunctionImplementation> functionsById = new ArrayList<>();
    private final Map<Lambda, FunctionImplementation> functionsByDefinition = new HashMap<>();

    private FunctionRegistry() {}

    public synchronized FunctionImplementation lookup(Lambda source) {
        return functionsByDefinition.get(source);
    }

    public synchronized FunctionImplementation lookupOrMake(Lambda source, FunctionImplementation topFunction) {
        return functionsByDefinition.computeIfAbsent(source, k -> new FunctionImplementation(source, topFunction));
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
