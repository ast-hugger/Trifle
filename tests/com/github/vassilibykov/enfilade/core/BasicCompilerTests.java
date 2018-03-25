// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("Convert2MethodRef")
public class BasicCompilerTests {
//
//    @Test
//    public void testConst() {
//        assertEquals(42, compile(const_(42)).invoke());
//    }
//
//    @Test
//    public void testArg() {
//        RunnableFunction function = unaryFunction(arg -> ref(arg));
//        function.nexus.forceCompile();
//        assertEquals(42, function.invoke(42));
//    }
//
//    @Test
//    public void testIf() {
//        RunnableFunction function = unaryFunction(
//            arg ->
//                if_(ref(arg),
//                    const_("true"),
//                    const_("false")));
//        function.nexus.forceCompile();
//        assertEquals("true", function.invoke(true));
//        assertEquals("false", function.invoke(false));
//    }
//
//    @Test
//    public void testLet() {
//        VariableDefinition t = var("t");
//        RunnableFunction function = unaryFunction(arg ->
//            let(t, ref(arg), ref(t)));
//        function.nexus.forceCompile();
//        assertEquals(42, function.invoke(42));
//        assertEquals("hello", function.invoke("hello"));
//    }
//
//    @Test
//    public void testLet2() {
//        VariableDefinition t = var("t");
//        VariableDefinition u = var("u");
//        RunnableFunction function = unaryFunction(arg ->
//            let(t, add(ref(arg), const_(1)),
//                let(u, add(ref(arg), const_(2)),
//                    mul(ref(t), ref(u)))));
//        function.nexus.forceCompile();
//        assertEquals(12, function.invoke(2));
//    }
//
//    @Test
//    public void testPrimitive1() {
//        RunnableFunction function = unaryFunction(
//            arg ->
//                negate(ref(arg)));
//        function.nexus.forceCompile();
//        assertEquals(-42, function.invoke(42));
//        assertEquals(123, function.invoke(-123));
//    }
//
//    @Test
//    public void testPrimitive2() {
//        RunnableFunction function = binaryFunction(
//            (arg1, arg2) ->
//                add(ref(arg1), ref(arg2)));
//        function.nexus.forceCompile();
//        assertEquals(7, function.invoke(3, 4));
//        assertEquals(0, function.invoke(-42, 42));
//    }
//
//    @Test
//    public void testSetVar() {
//        RunnableFunction function = unaryFunction(
//            arg ->
//                set(arg, const_(42)));
//        function.nexus.forceCompile();
//        assertEquals(42, function.invoke(3));
//    }
//
//    @Test
//    public void testSetVarInProg() {
//        RunnableFunction function = unaryFunction(
//            arg ->
//                prog(
//                    set(arg, const_(42)),
//                    ref(arg)));
//        function.nexus.forceCompile();
//        assertEquals(42, function.invoke(3));
//    }
//
//    @Test
//    public void testVar() {
//        RunnableFunction function = unaryFunction(arg -> ref(arg));
//        assertEquals(42, function.invoke(42));
//        assertEquals("hello", function.invoke("hello"));
//    }
//
//    @Test
//    public void testFactorial() {
//        RunnableFunction factorial = InterpreterEvaluationTests.factorial();
//        factorial.nexus.forceCompile();
//        assertEquals(6, factorial.invoke(3));
//        assertEquals(24, factorial.invoke(4));
//        assertEquals(120, factorial.invoke(5));
//    }
//
//    @Test
//    public void testFibonacci() { // and everybody's favorite
//        RunnableFunction fibonacci = InterpreterEvaluationTests.fibonacci();
//        fibonacci.nexus.forceCompile();
//        assertEquals(1, fibonacci.invoke(0));
//        assertEquals(1, fibonacci.invoke(1));
//        assertEquals(2, fibonacci.invoke(2));
//        assertEquals(3, fibonacci.invoke(3));
//        assertEquals(5, fibonacci.invoke(4));
//        assertEquals(8, fibonacci.invoke(5));
//        assertEquals(13, fibonacci.invoke(6));
//    }
//
////    @Test
//    public void timeFib() {
//        int n = 35;
//        RunnableFunction fibonacci = InterpreterEvaluationTests.fibonacci();
//        fibonacci.nexus.forceCompile();
//        Object[] args = {n};
//        for (int i = 0; i < 20; i++) fibonacci.invoke(n);
//        long start = System.nanoTime();
//        int result = (Integer) fibonacci.invoke(n);
//        long elapsed = System.nanoTime() - start;
//        System.out.format("fibonacci(%s) = %s in %s ms", n, result, elapsed / 1_000_000L);
//    }
//
//    private RunnableFunction compile(EvaluatorNode methodBody) {
//        RunnableFunction function = RunnableFunction.with(new VariableDefinition[0], methodBody);
//        function.nexus.forceCompile();
//        return function;
//    }
}
