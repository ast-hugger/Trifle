// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.add;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.binaryFunction;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.call;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.lessThan;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.let;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.mul;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.negate;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.nullaryFunction;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.prog;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.ref;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.set;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.sub;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.unaryFunction;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.var;
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
                if_(ref(arg),
                    const_("true"),
                    const_("false")));
        assertEquals("true", function.invoke(true));
        assertEquals("false", function.invoke(false));
    }

    @Test
    public void testLet() {
        Variable t = var("t");
        Variable u = var("u");
        Function function = nullaryFunction(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                    add(ref(t), ref(u)))));
        assertEquals(7, function.invoke());
    }

    @Test
    public void testPrimitive1() {
        Function function = unaryFunction(
            arg ->
                negate(ref(arg)));
        assertEquals(-42, function.invoke(42));
        assertEquals(123, function.invoke(-123));
    }

    @Test
    public void testPrimitive2() {
        Function function = binaryFunction(
            (arg1, arg2) ->
                add(ref(arg1), ref(arg2)));
        assertEquals(7, function.invoke(3, 4));
        assertEquals(0, function.invoke(-42, 42));
    }

    @Test
    public void testSetVar() {
        Function function = unaryFunction(
            arg ->
                set(arg, const_(42)));
        assertEquals(42, function.invoke(3));
    }

    @Test
    public void testSetVarInProg() {
        Function function = unaryFunction(
            arg ->
                prog(
                    set(arg, const_(42)),
                    ref(arg)));
        assertEquals(42, function.invoke(3));
    }

    @Test
    public void testVar() {
        Function function = unaryFunction(arg -> ref(arg));
        assertEquals(42, function.invoke(42));
        assertEquals("hello", function.invoke("hello"));
    }

    @Test
    public void testFactorial() {
        Function factorial = factorial();
        assertEquals(6, factorial.invoke(3));
        assertEquals(24, factorial.invoke(4));
        assertEquals(120, factorial.invoke(5));
    }

    @Test
    public void testFibonacci() { // and everybody's favorite
        Function fibonacci = fibonacci();
        assertEquals(1, fibonacci.invoke(0));
        assertEquals(1, fibonacci.invoke(1));
        assertEquals(2, fibonacci.invoke(2));
        assertEquals(3, fibonacci.invoke(3));
        assertEquals(5, fibonacci.invoke(4));
        assertEquals(8, fibonacci.invoke(5));
        assertEquals(13, fibonacci.invoke(6));
    }

    @Test
    public void testEvilFibonacci() {
        Function fibonacci = evilFibonacci();
        fibonacci.invoke(35); // enough to force compilation
        assertEquals(1, fibonacci.invoke(0));
        assertEquals(8, fibonacci.invoke(5));
        assertEquals("error", fibonacci.invoke(-1));
    }

    @Test
    public void testVeryEvilFibonacci() {
        Function fibonacci = veryEvilFibonacci();
        fibonacci.invoke(35); // enough to force compilation
        assertEquals(1, fibonacci.invoke(0));
        assertEquals(8, fibonacci.invoke(5));
        assertEquals("error", fibonacci.invoke(-1));
    }

    /*
        Support
     */

    private Object eval(Expression methodBody) {
        Function function = Function.with(methodBody);
        return function.invoke();
    }

    static Function factorial() {
        Variable n = var("n");
        Variable t = var("t");
        return Function.recursive(n, factorial ->
            if_(lessThan(ref(n), const_(1)),
                const_(1),
                let(t, call(factorial, sub(ref(n), const_(1))),
                    mul(ref(t), ref(n)))));
    }

    static Function fibonacci() {
        Variable n = var("n");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Function.recursive(n, fibonacci ->
            if_(lessThan(ref(n), const_(2)),
                const_(1),
                let(t1, call(fibonacci, sub(ref(n), const_(1))),
                    let(t2, call(fibonacci, sub(ref(n), const_(2))),
                        add(ref(t1), ref(t2))))));
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
            if_(lessThan(ref(n), const_(0)),
                const_("error"),
                if_(lessThan(ref(n), const_(2)),
                    const_(1),
                    let(t1, call(fibonacci, sub(ref(n), const_(1))),
                        let(t2, call(fibonacci, sub(ref(n), const_(2))),
                            add(ref(t1), ref(t2)))))));
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
            let(t0, if_(lessThan(ref(n), const_(0)),
                        const_("error"),
                        if_(lessThan(ref(n), const_(2)),
                            const_(1),
                            let(t1, call(fibonacci, sub(ref(n), const_(1))),
                                let(t2, call(fibonacci, sub(ref(n), const_(2))),
                                    add(ref(t1), ref(t2)))))),
                ref(t0)));
    }
}