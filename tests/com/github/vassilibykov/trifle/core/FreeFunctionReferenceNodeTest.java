// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.trifle.primitive.StandardPrimitiveLanguage.add;
import static org.junit.Assert.assertEquals;

public class FreeFunctionReferenceNodeTest {

    private Library library;

    @Before
    public void setUp() throws Exception {
        library = new Library();
        library.define("target", lambda(arg -> add(arg, arg)));
    }

    @Test
    public void profiledInterpretedUsedInCalledFunctionPosition() {
        library.define("caller", lambda(() -> call(library.at("target"), const_(3))));
        assertEquals(6, library.get("caller").invoke());
    }

    @Test
    public void simpleInterpretedUsedInCalledFunctionPosition() {
        library.define("caller", lambda(() -> call(library.at("target"), const_(3))));
        var caller = library.get("caller");
        caller.implementation().useSimpleInterpreter();
        assertEquals(6, caller.invoke());
    }

    @Test
    public void compiledUsedInCalledFunctionPosition() {
        library.define("caller", lambda(() -> call(library.at("target"), const_(3))));
        var caller = library.get("caller");
        caller.invoke();
        caller.implementation().forceCompile();
        assertEquals(6, caller.invoke());
    }

    @Test
    public void profiledInterpretedUsedAsClosure() {
        library.define("caller",
            lambda(() ->
                bind(library.at("target"), t ->
                    call(t, const_(3)))));
        var caller = library.get("caller");
        assertEquals(6, caller.invoke());
    }

    @Test
    public void simpleInterpretedUsedAsClosure() {
        library.define("caller",
            lambda(() ->
                bind(library.at("target"), t ->
                    call(t, const_(3)))));
        var caller = library.get("caller");
        caller.implementation().useSimpleInterpreter();
        assertEquals(6, caller.invoke());
    }

    @Test
    public void compiledUsedAsClosure() {
        library.define("caller",
            lambda(() ->
                bind(library.at("target"), t ->
                    call(t, const_(3)))));
        var caller = library.get("caller");
        caller.invoke();
        caller.implementation().forceCompile();
        assertEquals(6, caller.invoke());
    }
}
