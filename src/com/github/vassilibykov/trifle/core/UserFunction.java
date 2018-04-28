// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.expression.Lambda;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A user-defined function created and managed by {@link Library}.
 */
public class UserFunction implements FreeFunction {
    /**
     * Create a new user function by translating a lambda expression.
     *
     * @param name The name of the function. The name has no
     *        effect on the execution, other than allowing to look up
     *        a function in a library, if it is indeed contained in a
     *        library.
     * @param definition The lambda expression defining the function.
     * @return The newly created user function object.
     */
    public static UserFunction construct(@NotNull String name, Lambda definition) {
        var function = new UserFunction(name);
        function.implementation = FunctionTranslator.translate(definition);
        function.implementation.setUserFunction(function);
        return function;
    }

    /**
     * Create a new user function whose lambda expression may reference itself.
     *
     * @param name The name of the function. The name has no
     *        effect on the execution, other than allowing to look up
     *        a function in a library, if it is indeed contained in a
     *        library.
     * @param definer A Java function receiving the partially
     *        initialized UserFunction instance being created. The
     *        function should return a Lambda to use as the user
     *        function's definition.
     * @return The newly created user function object.
     */
    public static UserFunction construct(String name, Function<UserFunction, Lambda> definer) {
        var function = new UserFunction(name);
        var definition = definer.apply(function);
        function.implementation = FunctionTranslator.translate(definition);
        function.implementation.setUserFunction(function);
        return function;
    }

    /**
     * Create a group of functions which may be mutually recursive.
     * This is a generalization of {@link #construct(String, Function)}.
     *
     * @param names A list of function names.
     * @param definerDefiner A Java function which accepts a list of user function objects
     *        and returns a list of definition lambdas to use for their definitions.
     * @return A list of newly created user function objects.
     */
    public static List<UserFunction> construct(
        List<String> names,
        Function<List<UserFunction>, List<Lambda>> definerDefiner)
    {
        var functions = names.stream().map(UserFunction::new).collect(Collectors.toList());
        var definers = definerDefiner.apply(functions);
        if (definers.size() != functions.size()) {
            throw new IllegalArgumentException("lists don't match");
        }
        for (int i = 0; i < functions.size(); i++) { // where is my zipWith?
            var function = functions.get(i);
            var definition = definers.get(i);
            function.implementation = FunctionTranslator.translate(definition);
            function.implementation.setUserFunction(function);
        }
        return functions;
    }

    /*
        Instance
     */

    private final String name;
    private FunctionImplementation implementation; // non-null; always set by factory methods

    private UserFunction(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String name() {
        return name;
    }

    public FunctionImplementation implementation() {
        return implementation;
    }

    @Override
    public MethodHandle invoker(MethodType callSiteType) {
        return implementation.callSiteInvoker().asType(callSiteType);
    }

    @TestOnly
    public void useSimpleInterpreter() {
        implementation.useSimpleInterpreter();
    }

    @TestOnly
    public void forceCompile() {
        implementation.forceCompile();
    }
}
