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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class CallNodeTests {

    private Closure lambdaEcho;
    private FunctionImplementation lambdaEchoFunction;
    private CallNode lambdaEchoCall;
    private Closure directEcho;
    private FunctionImplementation directEchoFunction;
    private CallNode directEchoCall;

    @Before
    public void setUp() throws Exception {
        var topLevel = new TopLevel();
        topLevel.define("lambdaEcho",
            lambda(arg ->
                bind(lambda(x -> x), t ->
                    call(t, arg))));
        lambdaEcho = topLevel.getAsClosure("lambdaEcho");
        lambdaEchoFunction = lambdaEcho.implementation;
        lambdaEchoCall = (CallNode) ((LetNode) lambdaEchoFunction.body()).body();
        topLevel.define("echo", lambda(arg -> arg));
        topLevel.define("directEcho", lambda(arg -> call(topLevel.at("echo"), arg)));
        directEcho = topLevel.getAsClosure("directEcho");
        directEchoFunction = directEcho.implementation;
        directEchoCall = (CallNode) directEchoFunction.body();
    }

    private void profileAndCompileAll(Object arg) {
        lambdaEcho.invoke(arg);
        lambdaEchoFunction.forceCompile();
        directEcho.invoke(arg);
        directEchoFunction.forceCompile();
    }

    @Test
    public void profiledInterpretedEvaluationLambda() {
        assertEquals("hello", lambdaEcho.invoke("hello"));
        assertEquals(42, lambdaEcho.invoke(42));
    }

    @Test
    public void profiledInterpretedEvaluationDirect() {
        assertEquals("hello", directEcho.invoke("hello"));
        assertEquals(42, directEcho.invoke(42));
    }

    @Test
    public void simpleInterpretedEvaluationLambda() {
        lambdaEchoFunction.useSimpleInterpreter();
        assertEquals("hello", lambdaEcho.invoke("hello"));
        assertEquals(42, lambdaEcho.invoke(42));
    }

    @Test
    public void simpleInterpretedEvaluationDirect() {
        directEchoFunction.useSimpleInterpreter();
        assertEquals("hello", directEcho.invoke("hello"));
        assertEquals(42, directEcho.invoke(42));
    }

    @Test
    public void inferredType() {
        profileAndCompileAll("hello");
        assertTrue(lambdaEchoCall.inferredType().isUnknown());
        assertTrue(directEchoCall.inferredType().isUnknown());
    }

    @Test
    public void specializedTypeForString() {
        profileAndCompileAll("hello");
        assertEquals(REFERENCE, lambdaEchoCall.specializedType());
        assertEquals(REFERENCE, directEchoCall.specializedType());
    }

    @Test
    public void specializedTypeForInt() {
        profileAndCompileAll(42);
        assertEquals(INT, lambdaEchoCall.specializedType());
        assertEquals(INT, directEchoCall.specializedType());
    }

    @Test
    public void compiledEvaluationWithNoSpecializedTypeAvailableLambda() {
        lambdaEchoFunction.forceCompile();
        assertEquals("hello", lambdaEcho.invoke("hello"));
        assertEquals(42, lambdaEcho.invoke(42));
    }

    @Test
    public void compiledEvaluationWithNoSpecializedTypeAvailableDirect() {
        directEchoFunction.forceCompile();
        assertEquals("hello", directEcho.invoke("hello"));
        assertEquals(42, directEcho.invoke(42));
    }

    @Test
    public void compiledEvaluationWithSpecializedTypeAvailableLambda() {
        profileAndCompileAll(123);
        assertEquals(42, lambdaEcho.invoke(42));
        assertEquals("hello", lambdaEcho.invoke("hello"));
    }

    @Test
    public void compiledEvaluationWithSpecializedTypeAvailableDirect() {
        profileAndCompileAll(123);
        assertEquals(42, directEcho.invoke(42));
        assertEquals("hello", directEcho.invoke("hello"));
    }
}