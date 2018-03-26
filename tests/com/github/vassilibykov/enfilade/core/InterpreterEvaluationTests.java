// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Expression;
import com.github.vassilibykov.enfilade.expression.Function;
import com.github.vassilibykov.enfilade.expression.Variable;
import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.*;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("Convert2MethodRef")
public class InterpreterEvaluationTests {

    @Test
    public void testConstant() {
        assertEquals(42, eval(const_(42)));
        assertEquals(null, eval(const_(null)));
        assertEquals("hello", eval(const_("hello")));
    }

    @Test
    public void testIf() {
        Function function = unaryFunction(
            arg ->
                if_(arg,
                    const_("true"),
                    const_("false")));
        assertEquals("true", invoke(function, true));
        assertEquals("false", invoke(function, false));
    }

    @Test
    public void testLet() {
        Variable t = var("t");
        Variable u = var("u");
        Function function = nullaryFunction(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                    add(t, u))));
        assertEquals(7, invoke(function));
    }

    @Test
    public void testPrimitive1() {
        Function function = unaryFunction(
            arg ->
                negate(arg));
        assertEquals(-42, invoke(function, 42));
        assertEquals(123, invoke(function, -123));
    }

    @Test
    public void testPrimitive2() {
        Function function = binaryFunction(
            (arg1, arg2) ->
                add(arg1, arg2));
        assertEquals(7, invoke(function, 3, 4));
        assertEquals(0, invoke(function, -42, 42));
    }

    @Test
    public void testSetVar() {
        Function function = unaryFunction(
            arg ->
                set(arg, const_(42)));
        assertEquals(42, invoke(function, 3));
    }

    @Test
    public void testSetVarInProg() {
        Function function = unaryFunction(
            arg ->
                prog(
                    set(arg, const_(42)),
                    arg));
        assertEquals(42, invoke(function, 3));
    }

    @Test
    public void testVar() {
        Function function = unaryFunction(arg -> arg);
        assertEquals(42, invoke(function, 42));
        assertEquals("hello", invoke(function, "hello"));
    }

    @Test
    public void testFactorial() {
        Function factorial = factorial();
        assertEquals(6, invoke(factorial, 3));
        assertEquals(24, invoke(factorial, 4));
        assertEquals(120, invoke(factorial, 5));
    }

    @Test
    public void testFibonacci() { // and everybody's favorite
        Function fibonacci = fibonacci();
        assertEquals(1, invoke(fibonacci, 0));
        assertEquals(1, invoke(fibonacci, 1));
        assertEquals(2, invoke(fibonacci, 2));
        assertEquals(3, invoke(fibonacci, 3));
        assertEquals(5, invoke(fibonacci, 4));
        assertEquals(8, invoke(fibonacci, 5));
        assertEquals(13, invoke(fibonacci, 6));
    }

    @Test
    public void testEvilFibonacci() {
        Function fibonacci = evilFibonacci();
        invoke(fibonacci, 35); // enough to force compilation
        assertEquals(1, invoke(fibonacci, 0));
        assertEquals(8, invoke(fibonacci, 5));
        assertEquals("error", invoke(fibonacci, -1));
    }

    @Test
    public void testVeryEvilFibonacci() {
        Function fibonacci = veryEvilFibonacci();
        invoke(fibonacci, 35); // enough to force compilation
        assertEquals(1, invoke(fibonacci, 0));
        assertEquals(8, invoke(fibonacci, 5));
        assertEquals("error", invoke(fibonacci, -1));
    }

    /*
        Support
     */

    private Object eval(Expression methodBody) {
        Function function = Function.with(List.of(), methodBody);
        return invoke(function);
    }

    private Object invoke(Function function) {
        return FunctionTranslator.translate(function).invoke();
    }

    private Object invoke(Function function, Object arg) {
        return FunctionTranslator.translate(function).invoke(arg);
    }

    private Object invoke(Function function, Object arg1, Object arg2) {
        return FunctionTranslator.translate(function).invoke(arg1, arg2);
    }

    static Function factorial() {
        Variable n = var("n");
        Variable t = var("t");
        return Function.recursive(n, factorial ->
            if_(lessThan(n, const_(1)),
                const_(1),
                let(t, call(factorial, sub(n, const_(1))),
                    mul(t, n))));
    }

    static Function fibonacci() {
        Variable n = var("n");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Function.recursive(n, fibonacci ->
            if_(lessThan(n, const_(2)),
                const_(1),
                let(t1, call(fibonacci, sub(n, const_(1))),
                    let(t2, call(fibonacci, sub(n, const_(2))),
                        add(t1, t2)))));
    }

    /**
     * A version of fibonacci which profiles with all the same types as the
     * regular one, but includes an 'if' branch (not taken while being profiled)
     * which injects a value into computation incompatible with the profiled
     * types, and therefore with the specialized version of code.
     */
    static Function evilFibonacci() {
        Variable n = var("n");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Function.recursive(n, fibonacci ->
            if_(lessThan(n, const_(0)),
                const_("error"),
                if_(lessThan(n, const_(2)),
                    const_(1),
                    let(t1, call(fibonacci, sub(n, const_(1))),
                        let(t2, call(fibonacci, sub(n, const_(2))),
                            add(t1, t2))))));
    }

    /**
     * Here, unlike the simply evil version, the "error" constant is not in the
     * tail position, so the continuation which should but can't receive its
     * value lies within the function.
     */
    static Function veryEvilFibonacci() {
        Variable n = var("n");
        Variable t0 = var("t0");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Function.recursive(n, fibonacci ->
            let(t0, if_(lessThan(n, const_(0)),
                        const_("error"),
                        if_(lessThan(n, const_(2)),
                            const_(1),
                            let(t1, call(fibonacci, sub(n, const_(1))),
                                let(t2, call(fibonacci, sub(n, const_(2))),
                                    add(t1, t2))))),
                t0));
    }
}