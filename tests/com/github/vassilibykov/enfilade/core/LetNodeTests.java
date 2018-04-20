// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;
import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.*;

public class LetNodeTests {

    private Closure makeSupplier;
    private Closure intSupplier;
    private Closure stringSupplier;
    private Closure letClosure;
    private FunctionImplementation letFunction;
    private LetNode letNode;
    private VariableDefinition letVariable;

    @Before
    public void setUp() throws Exception {
        var topLevel = new TopLevel();
        topLevel.define("makeSupplier",
            lambda(arg ->
                lambda(() -> arg)));
        makeSupplier = topLevel.getAsClosure("makeSupplier");
        intSupplier = (Closure) makeSupplier.invoke(42);
        stringSupplier = (Closure) makeSupplier.invoke("hello");
        defineLetFunction(topLevel);
        letClosure = topLevel.getAsClosure("let");
        letFunction = letClosure.implementation;
        letNode = (LetNode) letFunction.body();
        letVariable = letNode.variable();
    }

    protected void defineLetFunction(TopLevel topLevel) {
        topLevel.define("let",
            lambda(f ->
                bind(call(f), t -> t)));
    }

    @Test
    public void profiledInterpretedEvaluation() {
        assertEquals(42, letClosure.invoke(intSupplier));
        assertEquals("hello", letClosure.invoke(stringSupplier));
    }

    @Test
    public void simpleInterpretedEvaluation() {
        letFunction.useSimpleInterpreter();
        assertEquals(42, letClosure.invoke(intSupplier));
        assertEquals("hello", letClosure.invoke(stringSupplier));
    }

    @Test
    public void inferredType() {
        profileAndCompile(intSupplier);
        assertTrue(letVariable.inferredType().isUnknown());
        assertTrue(letNode.inferredType().isUnknown());
    }

    @Test
    public void specializedTypeInt() {
        profileAndCompile(intSupplier);
        assertEquals(INT, letVariable.specializedType());
        assertEquals(INT, letNode.specializedType());
        assertEquals(INT, letFunction.specializedReturnType());
    }

    @Test
    public void specializedTypeString() {
        profileAndCompile(stringSupplier);
        assertEquals(REFERENCE, letVariable.specializedType());
        assertEquals(REFERENCE, letNode.specializedType());
        assertEquals(REFERENCE, letFunction.specializedReturnType());
    }

    @Test
    public void compiledEvaluationSameReference() {
        profileAndCompile(stringSupplier);
        assertEquals("hello", letClosure.invoke(stringSupplier)); // find call, binding and caching
        assertEquals("hello", letClosure.invoke(stringSupplier)); // second call using inline cache
    }

    @Test
    public void compiledEvaluationReferenceImmediateProfileFailure() {
        profileAndCompile(stringSupplier);
        assertEquals(42, letClosure.invoke(intSupplier));
    }

    @Test
    public void compiledEvaluationSameInt() {
        profileAndCompile(intSupplier);
        assertEquals(42, letClosure.invoke(intSupplier));
        assertEquals(42, letClosure.invoke(intSupplier));
    }

    @Test
    public void compiledEvaluationIntImmediateProfileFailure() {
        profileAndCompile(intSupplier);
        assertEquals("hello", letClosure.invoke(stringSupplier));
    }

    @Test
    public void compiledEvaluationWithRefToIntChange() {
        profileAndCompile(stringSupplier);
        assertEquals("hello", letClosure.invoke(stringSupplier)); // find call, binding and caching
        assertEquals("hello", letClosure.invoke(stringSupplier)); // second call using inline cache
        assertEquals(42, letClosure.invoke(intSupplier));
    }

    @Test
    public void compiledEvaluationWithIntToRefChange() {
        profileAndCompile(intSupplier);
        assertEquals(42, letClosure.invoke(intSupplier));
        assertEquals(42, letClosure.invoke(intSupplier));
        assertEquals("hello", letClosure.invoke(stringSupplier));
    }

    private void profileAndCompile(Closure valueSupplier) {
        letClosure.invoke(valueSupplier);
        letFunction.forceCompile();
    }
}