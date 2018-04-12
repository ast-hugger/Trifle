// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import com.github.vassilibykov.enfilade.core.Closure;
import com.github.vassilibykov.enfilade.core.FunctionTranslator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A collection of user-defined "global" functions.
 */
public class TopLevel {
    private Map<String, TopLevelFunction> topLevelFunctionsByName = new HashMap<>();

    public void define(String name, Function<TopLevelFunction, Lambda> definer) {
        var topItem = new TopLevelFunction();
        topLevelFunctionsByName.put(name, topItem);
        var definition = definer.apply(topItem);
        topItem.setImplementation(FunctionTranslator.translate(definition));
    }

    public void define(String name, Lambda definition) {
        var topItem = new TopLevelFunction();
        topLevelFunctionsByName.put(name, topItem);
        topItem.setImplementation(FunctionTranslator.translate(definition));
    }

    public TopLevelFunction get(String name) {
        return topLevelFunctionsByName.get(name);
    }

    public FunctionReference at(String name) {
        return FunctionReference.to(get(name));
    }

    public Closure getAsClosure(String name) { // FIXME for now, should not return closure
        var top =  topLevelFunctionsByName.get(name);
        return Closure.with(top.implementation());
    }
}
