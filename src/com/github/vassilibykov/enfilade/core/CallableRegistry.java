// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a mapping between callable IDs and {@link Callable}s.
 */
public class CallableRegistry {
    public static final CallableRegistry INSTANCE = new CallableRegistry();

    @SuppressWarnings("unused") // called by generated code
    public static Closure lookupAndMakeClosure(int functionId) {
        var callable = INSTANCE.lookup(functionId);
        if (!(callable instanceof FunctionImplementation)) throw new AssertionError();
        var function = (FunctionImplementation) callable;
        if (!function.isTopLevel()) throw new AssertionError();
        return new Closure(function, new Object[0]);
    }

    /*
        Instance
     */

    private final List<Callable> functionsById = new ArrayList<>();

    private CallableRegistry() {}

    public synchronized int register(Callable function) {
        if (function.id() >= 0) throw new AssertionError();
        int id = functionsById.size();
        functionsById.add(function);
        return id;
    }

    /**
     * Return the function with the specified ID.
     *
     * @throws IllegalArgumentException if there is not function with that ID.
     */
    public synchronized Callable lookup(int id) {
        try {
            return functionsById.get(id);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }

    public FunctionImplementation lookupFunctionImplementation(int id) {
        try {
            return (FunctionImplementation) lookup(id);
        } catch (ClassCastException e) {
            throw new AssertionError(e);
        }
    }

    public Closure lookupClosure(int id) {
        var callable = lookup(id);
        if (!(callable instanceof FunctionImplementation)) throw new AssertionError();
        return Closure.with((FunctionImplementation) callable);
    }
}
