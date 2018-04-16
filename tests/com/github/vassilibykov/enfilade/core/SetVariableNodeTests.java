// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;
import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SetVariableNodeTests {

    private Closure closure;
    private FunctionImplementation function;
    private VariableDefinition letVar;
    private SetVariableNode node;

    @Before
    public void setUp() throws Exception {
        var topLevel = new TopLevel();
        topLevel.define("letVarTest",
            lambda(arg ->
                bind(const_(1), t ->
                    block(
                        set(t, arg),
                        t))));
        closure = topLevel.getAsClosure("letVarTest");
        function = closure.implementation;
        var let = (LetNode) function.body();
        letVar = let.variable();
        var block = (BlockNode) let.body();
        node = (SetVariableNode) block.expressions()[0];
    }

    @Test
    public void profiledInterpretedEvaluation() {
        assertEquals(42, closure.invoke(42));
        assertEquals("hello", closure.invoke("hello"));
    }

    @Test
    public void simpleInterpretedEvaluation() {
        function.useSimpleInterpreter();
        assertEquals(42, closure.invoke(42));
        assertEquals("hello", closure.invoke("hello"));
    }

    @Test
    public void inferredType() {
        function.forceCompile();
        assertTrue(node.inferredType().isUnknown());
    }

    @Test
    public void specializedTypeIntCase() {
        closure.invoke(42);
        function.forceCompile();
        assertEquals(INT, node.specializedType());
        assertEquals(INT, letVar.specializedType());
    }

    @Test
    public void specializedTypeBoolCase() {
        closure.invoke(true);
        function.forceCompile();
        assertEquals(BOOL, node.specializedType());
        assertEquals(REFERENCE, letVar.specializedType());
    }

    @Test
    public void specializedTypeReferenceCase() {
        closure.invoke("hello");
        function.forceCompile();
        assertEquals(REFERENCE, node.specializedType());
        assertEquals(REFERENCE, letVar.specializedType());
    }

    @Test
    public void compiledEvaluationIntCase() {
        closure.invoke(123);
        function.forceCompile();
        assertEquals(42, closure.invoke(42));
    }

    @Test
    public void compiledEvaluationBoolCase() {
        closure.invoke(123);
        function.forceCompile();
        assertEquals(false, closure.invoke(false));
    }

    @Test
    public void compiledEvaluationReferenceCase() {
        closure.invoke(123);
        function.forceCompile();
        assertEquals("hello", closure.invoke("hello"));
    }
}
