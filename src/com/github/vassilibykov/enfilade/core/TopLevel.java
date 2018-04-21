// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.FreeFunctionReference;
import com.github.vassilibykov.enfilade.expression.Lambda;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A collection of user-defined "global" functions.
 */
public class TopLevel {
    private Map<String, UserFunction> topLevelFunctionsByName = new HashMap<>();

    public void define(String name, Function<UserFunction, Lambda> definer) {
        UserFunction.construct(function -> {
            topLevelFunctionsByName.put(name, function);
            var definition = definer.apply(function);
            return FunctionTranslator.translate(definition);
        });
    }

    public void define(String name, Lambda definition) {
        UserFunction.construct(it -> {
            topLevelFunctionsByName.put(name, it);
            return FunctionTranslator.translate(definition);
        });
    }

    public UserFunction get(String name) {
        return topLevelFunctionsByName.get(name);
    }

    public FreeFunctionReference at(String name) {
        return FreeFunctionReference.to(get(name));
    }

    public Closure getAsClosure(String name) { // FIXME for now, should not return closure
        var top =  topLevelFunctionsByName.get(name);
        return Closure.with(top.implementation());
    }
}
