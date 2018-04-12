// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;
import org.junit.Before;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class ClosureNodeTests {

    private Closure stringReturningClosureClosure;
    private FunctionImplementation stringReturningClosureFunction;
    private ClosureNode stringReturningClosureNode;
    private Closure freeVarReturningClosureClosure;
    private FunctionImplementation freeVarReturningClosureFunction;
    private ClosureNode freeVarReturningClosureNode;
    private FunctionImplementation freeVarReturningClosureImplementation;

    @Before
    public void setUp() throws Exception {
        var topLevel = new TopLevel();
        topLevel.define("stringReturning",
            lambda(() -> lambda(() -> const_("hello"))));
        stringReturningClosureClosure = topLevel.getAsClosure("stringReturning");
        stringReturningClosureFunction = stringReturningClosureClosure.implementation;
        stringReturningClosureNode = (ClosureNode) stringReturningClosureFunction.body();
        topLevel.define("freeVarReturning",
            lambda(arg -> lambda(() -> arg)));
        freeVarReturningClosureClosure = topLevel.getAsClosure("freeVarReturning");
        freeVarReturningClosureFunction = freeVarReturningClosureClosure.implementation;
        freeVarReturningClosureNode = (ClosureNode) freeVarReturningClosureFunction.body();
        freeVarReturningClosureImplementation = freeVarReturningClosureNode.function();
    }

    private void invokeAndCompileAll() {
        stringReturningClosureClosure.invoke();
        stringReturningClosureFunction.forceCompile();
        var closure = (Closure) freeVarReturningClosureClosure.invoke(123);
        // DO NOT use 123 in tests to be sure we are not accidentally copying it too early.
        closure.invoke(); // necessary so we can test the returned closure's specialization
        freeVarReturningClosureFunction.forceCompile();
    }

    @Test
    public void profiledInterpretedEvaluation() {
        var closure = (Closure) stringReturningClosureClosure.invoke();
        assertEquals("hello", closure.invoke());
        closure = (Closure) freeVarReturningClosureClosure.invoke(42);
        assertEquals(42, closure.invoke());
    }

    @Test
    public void simpleInterpretedEvaluation() {
        stringReturningClosureFunction.useSimpleInterpreter();
        var closure = (Closure) stringReturningClosureClosure.invoke();
        assertEquals("hello", closure.invoke());
        freeVarReturningClosureFunction.useSimpleInterpreter();
        closure = (Closure) freeVarReturningClosureClosure.invoke(42);
        assertEquals(42, closure.invoke());
    }

    @Test
    public void inferredType() {
        stringReturningClosureFunction.forceCompile(); // to infer types
        assertEquals(REFERENCE, stringReturningClosureNode.inferredType().jvmType().get());
        freeVarReturningClosureFunction.forceCompile();
        assertEquals(REFERENCE, freeVarReturningClosureNode.inferredType().jvmType().get());
        assertTrue(freeVarReturningClosureImplementation.body().inferredType().isUnknown());
    }

    @Test
    public void specializedType() {
        invokeAndCompileAll();
        assertEquals(REFERENCE, stringReturningClosureNode.specializedType());
        assertEquals(REFERENCE, stringReturningClosureFunction.specializedReturnType);
        assertEquals(REFERENCE, freeVarReturningClosureNode.specializedType());
        assertEquals(REFERENCE, freeVarReturningClosureFunction.specializedReturnType);
        assertEquals(INT, freeVarReturningClosureImplementation.body().specializedType());
        assertEquals(INT, freeVarReturningClosureImplementation.specializedReturnType);
    }

    @Test
    public void compiledEvaluation() {
        invokeAndCompileAll();
        var closure = (Closure) stringReturningClosureClosure.invoke();
        assertEquals("hello", closure.invoke());
        closure = (Closure) freeVarReturningClosureClosure.invoke(42);
        assertEquals(42, closure.invoke());
    }

    @Test
    public void genericImplementation() throws Throwable {
        invokeAndCompileAll();
        assertEquals(MethodType.genericMethodType(0), stringReturningClosureFunction.genericImplementation.type());
        var closure = (Closure) stringReturningClosureFunction.genericImplementation.invoke();
        assertEquals("hello", closure.invoke());
    }

    @Test
    public void specializedImplementation() throws Throwable {
        invokeAndCompileAll();
        MethodHandle implementation = freeVarReturningClosureImplementation.specializedImplementation;
        assertEquals(MethodType.methodType(int.class, int.class), implementation.type());
        // This is a raw invocation; the argument is the copied value of the closed over variable.
        assertEquals(42, implementation.invoke(42));
    }

    @Test
    public void closureGenericInvoker() throws Throwable {
        invokeAndCompileAll();
        var closure = (Closure) freeVarReturningClosureClosure.invoke(42);
        var invoker = closure.genericInvoker();
        assertEquals(42, invoker.invoke());
    }

    @Test
    public void optimalInvokerIntoReference() throws Throwable {
        invokeAndCompileAll();
        var closure = (Closure) freeVarReturningClosureClosure.invoke(42);
        var invoker = closure.optimalInvoker(MethodType.genericMethodType(0));
        assertEquals(42, invoker.invoke());
    }

    @Test
    public void optimalInvokerIntoInt() throws Throwable {
        invokeAndCompileAll();
        var closure = (Closure) freeVarReturningClosureClosure.invoke(42);
        var invoker = closure.optimalInvoker(MethodType.methodType(int.class));
        assertEquals(42, invoker.invoke());
    }

    @Test
    public void specializationFailureOfGenericInvoker() throws Throwable {
        invokeAndCompileAll();
        var closure = (Closure) freeVarReturningClosureClosure.invoke("hello");
        assertEquals("hello", closure.genericInvoker().invoke());
    }

    @Test
    public void specializationFailureOfOptimalInvokerIntoReference() throws Throwable {
        invokeAndCompileAll();
        var closure = (Closure) freeVarReturningClosureClosure.invoke("hello");
        var invoker = closure.optimalInvoker(MethodType.genericMethodType(0));
        assertEquals("hello", invoker.invoke());
    }

    @Test
    public void specializationFailureOfOptimalInvokerIntoInt() throws Throwable {
        invokeAndCompileAll();
        var closure = (Closure) freeVarReturningClosureClosure.invoke("hello");
        var invoker = closure.optimalInvoker(MethodType.methodType(int.class));
        Object result;
        try {
            result = invoker.invoke();
        } catch (SquarePegException e) {
            result = e.value;
        }
        assertEquals("hello", result);
    }
}