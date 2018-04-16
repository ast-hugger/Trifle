// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;
import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.add;
import static org.junit.Assert.assertEquals;

public class DirectFunctionNodeTest {

    private TopLevel topLevel;

    @Before
    public void setUp() throws Exception {
        topLevel = new TopLevel();
        topLevel.define("target", lambda(arg -> add(arg, arg)));
    }

    @Test
    public void profiledInterpretedUsedInCalledFunctionPosition() {
        topLevel.define("caller", lambda(() -> call(topLevel.at("target"), const_(3))));
        assertEquals(6, topLevel.getAsClosure("caller").invoke());
    }

    @Test
    public void simpleInterpretedUsedInCalledFunctionPosition() {
        topLevel.define("caller", lambda(() -> call(topLevel.at("target"), const_(3))));
        var caller = topLevel.getAsClosure("caller");
        caller.implementation.useSimpleInterpreter();
        assertEquals(6, caller.invoke());
    }

    @Test
    public void compiledUsedInCalledFunctionPosition() {
        topLevel.define("caller", lambda(() -> call(topLevel.at("target"), const_(3))));
        var caller = topLevel.getAsClosure("caller");
        caller.invoke();
        caller.implementation.forceCompile();
        assertEquals(6, caller.invoke());
    }

    @Test
    public void profiledInterpretedUsedAsClosure() {
        topLevel.define("caller",
            lambda(() ->
                bind(topLevel.at("target"), t ->
                    call(t, const_(3)))));
        var caller = topLevel.getAsClosure("caller");
        assertEquals(6, caller.invoke());
    }

    @Test
    public void simpleInterpretedUsedAsClosure() {
        topLevel.define("caller",
            lambda(() ->
                bind(topLevel.at("target"), t ->
                    call(t, const_(3)))));
        var caller = topLevel.getAsClosure("caller");
        caller.implementation.useSimpleInterpreter();
        assertEquals(6, caller.invoke());
    }

    @Test
    public void compiledUsedAsClosure() {
        topLevel.define("caller",
            lambda(() ->
                bind(topLevel.at("target"), t ->
                    call(t, const_(3)))));
        var caller = topLevel.getAsClosure("caller");
        caller.invoke();
        caller.implementation.forceCompile();
        assertEquals(6, caller.invoke());
    }
}
