package com.github.vassilibykov.enfilade;

import org.junit.Test;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static com.github.vassilibykov.enfilade.CodeFactory.*;
import static org.junit.Assert.assertEquals;

public class InterpreterEvaluationTests {

    private static final UnaryOperator<Object> NEGATE = a -> -((Integer) a);
    private static final BinaryOperator<Object> LESS_THAN = (a, b) -> ((Integer) a) < ((Integer) b);
    private static final BinaryOperator<Object> PLUS  = (a, b) -> ((Integer) a) + ((Integer) b);
    private static final BinaryOperator<Object> MINUS = (a, b) -> ((Integer) a) - ((Integer) b);
    private static final BinaryOperator<Object> TIMES = (a, b) -> ((Integer) a) * ((Integer) b);

    @Test
    public void testConstant() {
        assertEquals(42, eval(const_(42)));
        assertEquals(null, eval(const_(null)));
        assertEquals("hello", eval(const_("hello")));
    }

    @Test
    public void testIf() {
        Method method = unaryMethod(
            arg ->
                if_(arg,
                    const_("true"),
                const_("false")));
        assertEquals("true", method.invoke(true));
        assertEquals("false", method.invoke(false));
    }

    @Test
    public void testLet() {
        Var t = var("t");
        Var u = var("u");
        Method method = nullaryMethod(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                    primitive(PLUS, t, u))));
        assertEquals(7, method.invoke());
    }

    @Test
    public void testPrimitive1() {
        Method method = unaryMethod(
            arg ->
                primitive(NEGATE, arg));
        assertEquals(-42, method.invoke(42));
        assertEquals(123, method.invoke(-123));
    }

    @Test
    public void testPrimitive2() {
        Method method = binaryMethod(
            (arg1, arg2) ->
                primitive(PLUS, arg1, arg2));
        assertEquals(7, method.invoke(3, 4));
        assertEquals(0, method.invoke(-42, 42));
    }

    @Test
    public void testSetVar() {
        Method method = unaryMethod(
            arg ->
                set(arg, const_(42)));
        assertEquals(42, method.invoke(3));
    }

    @Test
    public void testSetVarInProg() {
        Method method = unaryMethod(
            arg ->
                prog(
                    set(arg, const_(42)),
                    arg));
        assertEquals(42, method.invoke(3));
    }

    @Test
    public void testVar() {
        Method method = unaryMethod(arg -> arg);
        assertEquals(42, method.invoke(42));
        assertEquals("hello", method.invoke("hello"));
    }

    @Test
    public void testFactorial() {
        Method factorial = factorial();
        assertEquals(6, factorial.invoke(3));
        assertEquals(24, factorial.invoke(4));
        assertEquals(120, factorial.invoke(5));
    }

    @Test
    public void testFibonacci() { // and everybody's favorite
        Method fibonacci = fibonacci();
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
        Method fibonacci = fibonacci();
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
        Method method = Method.with(new Var[0], methodBody);
        return method.invoke();
    }

    private Method factorial() {
        Var n = var("n");
        Var t = var("t");
        return Method.withRecursion(new Var[]{n}, rec ->
            if_(primitive(LESS_THAN, n, const_(1)),
                const_(1),
                let(t, call(rec, primitive(MINUS, n, const_(1))),
                    primitive(TIMES, t, n))));
    }

    private Method fibonacci() {
        Var n = var("n");
        Var t1 = var("t1");
        Var t2 = var("t2");
        return Method.withRecursion(new Var[]{n}, rec ->
            if_(primitive(LESS_THAN, n, const_(2)),
                const_(1),
                let(t1, call(rec, primitive(MINUS, n, const_(1))),
                    let(t2, call(rec, primitive(MINUS, n, const_(2))),
                        primitive(PLUS, t1, t2)))));
    }
}