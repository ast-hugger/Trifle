// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Variable;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.vassilibykov.enfilade.expression.Function;

import java.util.List;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static org.junit.Assert.assertEquals;

public class InterpreterProfilingTests {

    private static Interpreter interpreter;

    @BeforeClass
    public static void setUpClass() {
        interpreter = new Interpreter();
    }

    @Test
    public void testInvocationsCount() {
        Function callee = nullaryFunction(() -> const_(42));
        Function caller = nullaryFunction(() ->
            prog(
                call(callee),
                call(callee)));
        RunnableFunction runnableCallee = FunctionTranslator.translate(callee);
        RunnableFunction runnableCaller = FunctionTranslator.translate(caller);
        for (int i = 0; i < 7; i++) {
            runnableCaller.invoke();
        }
        assertEquals(7, runnableCaller.profile.invocationCount());
        assertEquals(14, runnableCaller.profile.invocationCount());
    }

    @Test
    public void testMethodArgTypeProfile() {
        Variable arg = var("arg");
        Function function = Function.with(List.of(arg), arg);
        Variable arg2 = var("arg2");
        Function function2 = Function.with(List.of(arg2),
            prog(
                call(function, arg2),
                call(function, arg2)));
        RunnableFunction runnable1 = FunctionTranslator.translate(function);
        RunnableFunction runnable2 = FunctionTranslator.translate(function2);
        for (int i = 0; i < 3; i++) {
            runnable2.invoke("hello");
        }
        for (int i = 0; i < 4; i++) {
            runnable2.invoke(42);
        }
        assertEquals(3, runnable2.arguments()[0].profile.referenceCases());
        assertEquals(4, runnable2.arguments()[0].profile.intCases());
        assertEquals(6, runnable1.arguments()[0].profile.referenceCases());
        assertEquals(8, runnable1.arguments()[0].profile.intCases());
    }

    @Test
    public void testLetVarProfile() {
//        Variable arg = var("arg");
//        Variable t = var("t");
//        Function function = Function.with(new VariableDefinition[]{arg},
//            let(t, ref(arg), ref(t)));
//        for (int i = 0; i < 3; i++) {
//            function.invoke("hello");
//        }
//        for (int i = 0; i < 4; i++) {
//            function.invoke(42);
//        }
//        assertEquals(3, t.profile.referenceCases());
//        assertEquals(4, t.profile.intCases());
    }
}
