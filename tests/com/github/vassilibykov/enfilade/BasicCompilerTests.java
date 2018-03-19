package com.github.vassilibykov.enfilade;

import org.junit.Test;

import static com.github.vassilibykov.enfilade.ExpressionLanguage.*;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("Convert2MethodRef")
public class BasicCompilerTests {

    @Test
    public void testConst() {
        assertEquals(42, compile(const_(42)).invoke());
    }

    @Test
    public void testArg() {
        Function function = unaryFunction(arg -> arg);
        function.nexus.forceCompile();
        assertEquals(42, function.invoke(42));
    }

    @Test
    public void testIf() {
        Function function = unaryFunction(
            arg ->
                if_(arg,
                    const_("true"),
                    const_("false")));
        function.nexus.forceCompile();
        assertEquals("true", function.invoke(true));
        assertEquals("false", function.invoke(false));
    }

    @Test
    public void testLet() {
        Var t = var("t");
        Function function = unaryFunction(arg ->
            let(t, arg, t));
        function.nexus.forceCompile();
        assertEquals(42, function.invoke(42));
        assertEquals("hello", function.invoke("hello"));
    }

    @Test
    public void testLet2() {
        Var t = var("t");
        Var u = var("u");
        Function function = unaryFunction(arg ->
            let(t, add(arg, const_(1)),
                let(u, add(arg, const_(2)),
                    mul(t, u))));
        function.nexus.forceCompile();
        assertEquals(12, function.invoke(2));
    }

    @Test
    public void testPrimitive1() {
        Function function = unaryFunction(
            arg ->
                negate(arg));
        function.nexus.forceCompile();
        assertEquals(-42, function.invoke(42));
        assertEquals(123, function.invoke(-123));
    }

    @Test
    public void testPrimitive2() {
        Function function = binaryFunction(
            (arg1, arg2) ->
                add(arg1, arg2));
        function.nexus.forceCompile();
        assertEquals(7, function.invoke(3, 4));
        assertEquals(0, function.invoke(-42, 42));
    }

    @Test
    public void testSetVar() {
        Function function = unaryFunction(
            arg ->
                set(arg, const_(42)));
        function.nexus.forceCompile();
        assertEquals(42, function.invoke(3));
    }

    @Test
    public void testSetVarInProg() {
        Function function = unaryFunction(
            arg ->
                prog(
                    set(arg, const_(42)),
                    arg));
        function.nexus.forceCompile();
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
        Function factorial = InterpreterEvaluationTests.factorial();
        factorial.nexus.forceCompile();
        assertEquals(6, factorial.invoke(3));
        assertEquals(24, factorial.invoke(4));
        assertEquals(120, factorial.invoke(5));
    }

    @Test
    public void testFibonacci() { // and everybody's favorite
        Function fibonacci = InterpreterEvaluationTests.fibonacci();
        fibonacci.nexus.forceCompile();
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
        Function fibonacci = InterpreterEvaluationTests.fibonacci();
        fibonacci.nexus.forceCompile();
        Object[] args = {n};
        for (int i = 0; i < 20; i++) fibonacci.invoke(n);
        long start = System.nanoTime();
        int result = (Integer) fibonacci.invoke(n);
        long elapsed = System.nanoTime() - start;
        System.out.format("fibonacci(%s) = %s in %s ms", n, result, elapsed / 1_000_000L);
    }

    private Function compile(Expression methodBody) {
        Function function = Function.with(new Var[0], methodBody);
        function.nexus.forceCompile();
        return function;
    }
}
