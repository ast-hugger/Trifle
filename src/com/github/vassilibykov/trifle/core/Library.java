// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.expression.FreeFunctionReference;
import com.github.vassilibykov.trifle.expression.Lambda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A collection of user-defined named functions with one-way immutability of
 * name-to-function bindings. New functions can be added to the library, but
 * once added a function by a particular name can not be removed or replaced.
 *
 * <p>There is no special significance to a library other than being essentially
 * a map from function names to functions. A user function needs not be
 * contained in a library. A library may be used for something not normally
 * thought of as a library--for example, a method dictionary of a Smalltalk
 * class (if the immutability is not a problem).
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
    public synchronized UserFunction define(String name, Lambda definition) {
        if (functionsByName.containsKey(name)) {
            throw new IllegalArgumentException("function already defined: " + name);
        }
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
    public synchronized UserFunction define(String name, Function<UserFunction, Lambda> definer) {
        if (functionsByName.containsKey(name)) {
            throw new IllegalArgumentException("function already defined: " + name);
        }
        return UserFunction.construct(name, function -> {
            /* The function must be added to the map before running the definer
               so the definer may use #get() and #at() to reference it. */
            functionsByName.put(name, function);
            return definer.apply(function);
        });
    }

    /**
     * Define a set of functions which may be mutually recursive.
     *
     * @param names A list of function names.
     * @param definers A list of Java functions which must be of the same size
     *        as the list of names. Each Java function accepts a user function
     *        object and produces its definition lambda. At the time of each
     *        definer invocation, the library already contains bindings for all
     *        names.
     * @return The newly created user function objects.
     */
    public synchronized List<UserFunction> define(List<String> names, List<Function<UserFunction, Lambda>> definers) {
        if (names.size() != definers.size()) {
            throw new IllegalArgumentException("lists don't match");
        }
        for (var name : names) {
            if (functionsByName.containsKey(name)) {
                throw new IllegalArgumentException("function already defined: " + name);
            }
        }
        var definerIterator = definers.iterator();
        return UserFunction.construct(names, functions -> {
            functions.forEach(each -> functionsByName.put(each.name(), each));
            return functions.stream()
                .map(each -> definerIterator.next().apply(each))
                .collect(Collectors.toList());
        });
    }


    /**
     * Return a user function by the specified name.
     *
     * @throws NullPointerException If there is no function by that name.
     */
    public synchronized UserFunction get(String name) {
        return Objects.requireNonNull(functionsByName.get(name));
    }

    /**
     * Return as an Optional a user function by the specified name.
     */
    public synchronized Optional<UserFunction> getOptional(String name) {
        return Optional.ofNullable(functionsByName.get(name));
    }

    /**
     * Return a {@link FreeFunctionReference} expression referencing a function
     * by the specified name.
     *
     * @throws NullPointerException If there is no function by that name.
     */
    public synchronized FreeFunctionReference at(String name) {
        return FreeFunctionReference.to(get(name));
    }
}
