// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Expression;
import com.github.vassilibykov.enfilade.expression.ExpressionLanguage;
import com.github.vassilibykov.enfilade.expression.Lambda;
import org.junit.Assert;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.let;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.set;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.var;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.add;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.lessThan;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.mul;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.negate;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.sub;
import static org.junit.Assert.assertEquals;

public abstract class LanguageFeaturesTest {

    protected abstract Object eval(Expression expression);
    protected abstract Object invoke(Lambda function, Object... args);

    @Test
    public void testConstant() {
        Assert.assertEquals(42, eval(const_(42)));
        Assert.assertEquals(null, eval(const_(null)));
        Assert.assertEquals("hello", eval(const_("hello")));
    }

    @Test
    public void testIf() {
        var function = lambda(
            arg ->
                if_(arg,
                    const_("true"),
                    const_("false")));
        Assert.assertEquals("true", invoke(function, true));
        Assert.assertEquals("false", invoke(function, false));
    }

    @Test
    public void testLet() {
        var t = var("t");
        var u = var("u");
        var function = lambda(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                    add(t, u))));
        Assert.assertEquals(7, invoke(function));
    }

    @Test
    public void testNonlocalReference() {
        var t = var("t");
        var function = lambda(a ->
            let(t, lambda(b -> add(a, b)),
                call(t, const_(4))));
        assertEquals(7, invoke(function, 3));
    }

    @Test
    public void test2LevelNonlocalReference() {
        var t = var("t");
        var u = var("u");
        var function = lambda(a ->
            let(t, lambda(b ->
                let(u, lambda(c -> add(a, add(b, c))),
                    call(u, const_(5)))),
                call(t, const_(4))));
        assertEquals(12, invoke(function, 3));
    }

    @Test
    public void testPrimitive1() {
        var function = lambda(
            arg ->
                negate(arg));
        assertEquals(-42, invoke(function, 42));
        assertEquals(123, invoke(function, -123));
    }

    @Test
    public void testPrimitive2() {
        var function = ExpressionLanguage.lambda(
            (arg1, arg2) ->
                add(arg1, arg2));
        assertEquals(7, invoke(function, 3, 4));
        assertEquals(0, invoke(function, -42, 42));
    }

    @Test
    public void testSetVar() {
        var function = lambda(
            arg ->
                set(arg, const_(42)));
        assertEquals(42, invoke(function, 3));
    }

    @Test
    public void testSetVarInProg() {
        var function = lambda(
            arg ->
                block(
                    set(arg, const_(42)),
                    arg));
        assertEquals(42, invoke(function, 3));
    }

    @Test
    public void testSetNonlocalVar() {
        var t = var("t");
        var function = lambda(
            arg ->
                let(t, lambda(x -> set(arg, x)),
                    block(
                        call(t, const_(42)),
                        arg)));
        assertEquals(42, invoke(function, 3));
    }

    @Test
    public void testVar() {
        var function = lambda(arg -> arg);
        assertEquals(42, invoke(function, 42));
        assertEquals("hello", invoke(function, "hello"));
    }

    @Test
    public void testFactorial() {
        var factorial = factorial();
        assertEquals(6, invoke(factorial, 3));
        assertEquals(24, invoke(factorial, 4));
        assertEquals(120, invoke(factorial, 5));
    }

    @Test
    public void testFibonacci() { // and everybody's favorite
        var fibonacci = LanguageFeaturesTest.fibonacci();
        assertEquals(1, invoke(fibonacci, 0));
        assertEquals(1, invoke(fibonacci, 1));
        assertEquals(2, invoke(fibonacci, 2));
        assertEquals(3, invoke(fibonacci, 3));
        assertEquals(5, invoke(fibonacci, 4));
        assertEquals(8, invoke(fibonacci, 5));
        assertEquals(13, invoke(fibonacci, 6));
    }

    static Lambda factorial() {
        var factorial = var("factorial");
        var t = var("t");
        return lambda(arg ->
            let(factorial, const_(null),
                block(
                    set(factorial, lambda(n ->
                        if_(lessThan(n, const_(1)),
                            const_(1),
                            let(t, call(factorial, sub(n, const_(1))),
                                mul(t, n))))),
                    call(factorial, arg))));
    }

    static Lambda fibonacci() {
        var fibonacci = var("fibonacci");
        var t1 = var("t1");
        var t2 = var("t2");
        return lambda(arg ->
            let(fibonacci, const_(null),
                block(
                    set(fibonacci, lambda(n ->
                        if_(lessThan(n, const_(2)),
                            const_(1),
                            let(t1, call(fibonacci, sub(n, const_(1))),
                                let(t2, call(fibonacci, sub(n, const_(2))),
                                    add(t1, t2)))))),
                    call(fibonacci, arg))));
    }
}
