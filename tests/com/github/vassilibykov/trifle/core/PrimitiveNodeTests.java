// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.trifle.core.JvmType.INT;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.trifle.primitive.StandardPrimitiveLanguage.add;
import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class PrimitiveNodeTests {

    private UserFunction closure;
    private FunctionImplementation function;
    private Primitive2Node node;

    @Before
    public void setUp() throws Exception {
        var topLevel = new Library();
        topLevel.define("test",
            lambda((a, b) -> add(a, b)));
        closure = topLevel.get("test");
        function = closure.implementation();
        node = (Primitive2Node) function.body();
    }

    @Test
    public void profiledInterpretedEvaluation() {
        assertEquals(7, closure.invoke(3, 4));
    }

    @Test(expected = RuntimeError.class)
    public void profiledInterpretedEvaluationBadArg() {
        assertEquals(7, closure.invoke(3, "four"));
    }

    @Test
    public void simpleInterpretedEvaluation() {
        function.useSimpleInterpreter();
        assertEquals(7, closure.invoke(3, 4));
    }

    @Test(expected = RuntimeError.class)
    public void simpleInterpretedEvaluationBadArg() {
        function.useSimpleInterpreter();
        assertEquals(7, closure.invoke(3, "four"));
    }

    @Test
    public void inferredType() {
        function.forceCompile();
        assertEquals(INT, node.inferredType().jvmType().get());
    }

    @Test
    public void specializedType() {
        closure.invoke(3, 4);
        function.forceCompile();
        assertEquals(INT, node.specializedType());
    }

    @Test
    public void compiledEvaluation() {
        closure.invoke(1, 2);
        function.forceCompile();
        assertEquals(7, closure.invoke(3, 4));
    }

    @Test(expected = RuntimeError.class)
    public void compiledEvaluationBadArg() {
        closure.invoke(1, 2);
        function.forceCompile();
        assertEquals(7, closure.invoke(3, "four"));
    }
}