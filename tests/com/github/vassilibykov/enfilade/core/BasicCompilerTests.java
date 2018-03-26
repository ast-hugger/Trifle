// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Expression;
import com.github.vassilibykov.enfilade.expression.Variable;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.add;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.mul;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.negate;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("Convert2MethodRef")
public class BasicCompilerTests {

    @Test
    public void testConst() {
        assertEquals(42, compile(const_(42)).invoke());
    }

    @Test
    public void testArg() {
        RuntimeFunction function = FunctionTranslator.translate(unaryFunction(arg -> arg));
        function.forceCompile();
        assertEquals(42, function.invoke(42));
    }

    @Test
    public void testIf() {
        RuntimeFunction function = FunctionTranslator.translate(
            unaryFunction(
                arg ->
                    if_(arg,
                        const_("true"),
                        const_("false"))));
        function.forceCompile();
        assertEquals("true", function.invoke(true));
        assertEquals("false", function.invoke(false));
    }

    @Test
    public void testLet() {
        Variable t = var("t");
        RuntimeFunction function = FunctionTranslator.translate(
            unaryFunction(arg -> let(t, arg, t)));
        function.forceCompile();
        assertEquals(42, function.invoke(42));
        assertEquals("hello", function.invoke("hello"));
    }

    @Test
    public void testLet2() {
        Variable t = var("t");
        Variable u = var("u");
        RuntimeFunction function = FunctionTranslator.translate(
            unaryFunction(arg ->
            let(t, add(arg, const_(1)),
                let(u, add(arg, const_(2)),
                    mul(t, u)))));
        function.forceCompile();
        assertEquals(12, function.invoke(2));
    }

    @Test
    public void testPrimitive1() {
        RuntimeFunction function = FunctionTranslator.translate(
            unaryFunction(
                arg ->
                    negate(arg)));
        function.forceCompile();
        assertEquals(-42, function.invoke(42));
        assertEquals(123, function.invoke(-123));
    }

    @Test
    public void testPrimitive2() {
        RuntimeFunction function = FunctionTranslator.translate(
            binaryFunction(
                (arg1, arg2) ->
                    add(arg1, arg2)));
        function.forceCompile();
        assertEquals(7, function.invoke(3, 4));
        assertEquals(0, function.invoke(-42, 42));
    }

    @Test
    public void testSetVar() {
        RuntimeFunction function = FunctionTranslator.translate(
            unaryFunction(
                arg ->
                    set(arg, const_(42))));
        function.forceCompile();
        assertEquals(42, function.invoke(3));
    }

    @Test
    public void testSetVarInProg() {
        RuntimeFunction function = FunctionTranslator.translate(
            unaryFunction(
                arg ->
                    prog(
                        set(arg, const_(42)),
                        arg)));
        function.forceCompile();
        assertEquals(42, function.invoke(3));
    }

    @Test
    public void testVar() {
        RuntimeFunction function = FunctionTranslator.translate(unaryFunction(arg -> arg));
        assertEquals(42, function.invoke(42));
        assertEquals("hello", function.invoke("hello"));
    }

    @Test
    public void testFactorial() {
        RuntimeFunction factorial = FunctionTranslator.translate(InterpreterEvaluationTests.factorial());
        factorial.forceCompile();
        assertEquals(6, factorial.invoke(3));
        assertEquals(24, factorial.invoke(4));
        assertEquals(120, factorial.invoke(5));
    }

    @Test
    public void testFibonacci() { // and everybody's favorite
        RuntimeFunction fibonacci = FunctionTranslator.translate(InterpreterEvaluationTests.fibonacci());
        fibonacci.forceCompile();
        assertEquals(1, fibonacci.invoke(0));
        assertEquals(1, fibonacci.invoke(1));
        assertEquals(2, fibonacci.invoke(2));
        assertEquals(3, fibonacci.invoke(3));
        assertEquals(5, fibonacci.invoke(4));
        assertEquals(8, fibonacci.invoke(5));
        assertEquals(13, fibonacci.invoke(6));
    }

//    @Test
    public void timeFib() {
        int n = 35;
        RuntimeFunction fibonacci = FunctionTranslator.translate(InterpreterEvaluationTests.fibonacci());
        fibonacci.forceCompile();
        Object[] args = {n};
        for (int i = 0; i < 20; i++) fibonacci.invoke(n);
        long start = System.nanoTime();
        int result = (Integer) fibonacci.invoke(n);
        long elapsed = System.nanoTime() - start;
        System.out.format("fibonacci(%s) = %s in %s ms", n, result, elapsed / 1_000_000L);
    }

    private RuntimeFunction compile(Expression methodBody) {
        RuntimeFunction function = FunctionTranslator.translate(nullaryFunction(() -> methodBody));
        function.forceCompile();
        return function;
    }
}
