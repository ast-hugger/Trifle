// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.expression.FreeFunctionReference;
import com.github.vassilibykov.trifle.expression.Lambda;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A collection of user-defined named functions. There is no special
 * significance to a library other than being essentially a map from function
 * names to functions. A user function needs not be contained in a library.
 * A library may be used for something not normally thought of as a library--
 * for example, a method dictionary of a Smalltalk class.
 */
public class Library {
    private Map<String, UserFunction> functionsByName = new HashMap<>();

    /**
     * Define a new function. The function may not be recursive.
     *
     * @param name The name of the function. Must not be defined yet.
     * @param definition The definition of the function.
     * @return The newly created user function object.
     * @throws IllegalArgumentException If a function by that name already exists.
     */
    public UserFunction define(String name, Lambda definition) {
        if (functionsByName.containsKey(name)) throw new IllegalArgumentException();
        var function = UserFunction.construct(name, definition);
        functionsByName.put(name, function);
        return function;
    }

    /**
     * Define a new function which may be recursive.
     *
     * @param name The name of the function. Must not be defined yet.
     * @param definer A Java function which accepts the partially initialized
     *        UserFunction instance being created and returns the function
     *        definition. Because the UserFunction is available in the definer,
     *        the definition may reference the function being defined.
     * @return the newly created user function object.
     * @throws IllegalArgumentException If a function by that name already exists.
     */
    public UserFunction define(String name, Function<UserFunction, Lambda> definer) {
        if (functionsByName.containsKey(name)) throw new IllegalArgumentException();
        return UserFunction.construct(name, function -> {
            /* The function must be added to the map before running the definer
               so the definer may use #get() and #at() to reference it. */
            functionsByName.put(name, function);
            return definer.apply(function);
        });
    }

    /**
     * Return a user function by the specified name.
     *
     * @throws NullPointerException If there is no function by that name.
     */
    public UserFunction get(String name) {
        return Objects.requireNonNull(functionsByName.get(name));
    }

    /**
     * Return a {@link FreeFunctionReference} expression referencing a function
     * by the specified name.
     *
     * @throws NullPointerException If there is no function by that name.
     */
    public FreeFunctionReference at(String name) {
        return FreeFunctionReference.to(get(name));
    }
}
