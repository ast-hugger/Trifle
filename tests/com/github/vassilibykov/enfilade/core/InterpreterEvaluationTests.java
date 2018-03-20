package com.github.vassilibykov.enfilade.core;

import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.*;
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
        assertEquals("true", function.invoke(true));
        assertEquals("false", function.invoke(false));
    }

    @Test
    public void testLet() {
        Var t = var("t");
        Var u = var("u");
        Function function = nullaryFunction(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                    add(t, u))));
        assertEquals(7, function.invoke());
    }

    @Test
    public void testPrimitive1() {
        Function function = unaryFunction(
            arg ->
                negate(arg));
        assertEquals(-42, function.invoke(42));
        assertEquals(123, function.invoke(-123));
    }

    @Test
    public void testPrimitive2() {
        Function function = binaryFunction(
            (arg1, arg2) ->
                add(arg1, arg2));
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
                    arg));
        assertEquals(42, function.invoke(3));
    }

    @Test
    public void testVar() {
        Function function = unaryFunction(arg -> arg);
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

//    @Test
    public void timeFib() {
        // Now that the compiler is enabled, this in fact doesn't time the interpreter,
        // as most of the computation is done in compiled mode.
        int n = 35;
        Function fibonacci = fibonacci();
        Object[] args = {n};
        for (int i = 0; i < 20; i++) fibonacci.invoke(n);
        long start = System.nanoTime();
        int result = (Integer) fibonacci.invoke(n);
        long elapsed = System.nanoTime() - start;
        System.out.format("fibonacci(%s) = %s in %s ms", n, result, elapsed / 1_000_000L);
    }

    /*
        Support
     */

    private Object eval(Expression methodBody) {
        Function function = Function.with(new Var[0], methodBody);
        return function.invoke();
    }

    static Function factorial() {
        Var n = var("n");
        Var t = var("t");
        return Function.withRecursion(new Var[]{n}, factorial ->
            if_(lessThan(n, const_(1)),
                const_(1),
                let(t, call(factorial, sub(n, const_(1))),
                    mul(t, n))));
    }

    static Function fibonacci() {
        Var n = var("n");
        Var t1 = var("t1");
        Var t2 = var("t2");
        return Function.withRecursion(new Var[]{n}, fibonacci ->
            if_(lessThan(n, const_(2)),
                const_(1),
                let(t1, call(fibonacci, sub(n, const_(1))),
                    let(t2, call(fibonacci, sub(n, const_(2))),
                        add(t1, t2)))));
    }
}