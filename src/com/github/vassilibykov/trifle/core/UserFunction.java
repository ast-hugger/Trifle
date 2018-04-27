// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.expression.Lambda;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Function;

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
        return new UserFunction(name, definition);
    }

    /**
     * Create a new user function in such a way that the lambda defining the
     * user function is supplied by a Java function which receives the
     * user-function-to-be. This approach allows defining recursive functions
     * using {@link Library#define(String, Function)} and at the same time keep
     * immutable all the relevant fields in the classes involved.
     *
     * @param name The name of the function. The name has no
     *        effect on the execution, other than allowing to look up
     *        a function in a library, if it is indeed contained in a
     *        library.
     * @param definitionMaker A Java function receiving the partially
     *        initialized UserFunction instance being created. The
     *        function should return a Lambda to use as the user
     *        function's definition.
     * @return The newly created user function object.
     */
    public static UserFunction construct(String name, Function<UserFunction, Lambda> definitionMaker) {
        return new UserFunction(name, definitionMaker);
    }

    /*
        Instance
     */

    @NotNull private final String name;
    @NotNull private final FunctionImplementation implementation;

    public UserFunction(@NotNull String name, @NotNull Lambda definition) {
        this.name = name;
        this.implementation = FunctionTranslator.translate(definition);
        this.implementation.setUserFunction(this);
    }

    private UserFunction(@NotNull String name, Function<UserFunction, Lambda> definitionMaker) {
        this.name = name;
        var definition = definitionMaker.apply(this);
        this.implementation = FunctionTranslator.translate(definition);
        this.implementation.setUserFunction(this);
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
