// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.ExpressionLanguage;
import com.github.vassilibykov.enfilade.expression.Variable;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.github.vassilibykov.enfilade.expression.Lambda;

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
        Lambda callee = ExpressionLanguage.lambda(() -> const_(42));
        Lambda caller = ExpressionLanguage.lambda(() ->
            block(
                call(callee),
                call(callee)));
        Closure runnableCallee = FunctionTranslator.translate(callee);
        Closure runnableCaller = FunctionTranslator.translate(caller);
        for (int i = 0; i < 7; i++) {
            runnableCaller.invoke();
        }
        assertEquals(7, runnableCaller.implementation.profile.invocationCount());
        assertEquals(14, runnableCallee.implementation.profile.invocationCount());
    }

    @Ignore("ignored because this kind of function reference doesn't work anymore")
    @Test
    public void testMethodArgTypeProfile() {
        Variable arg = var("arg");
        Lambda function = Lambda.with(List.of(arg), arg);
        Variable arg2 = var("arg2");
        Lambda function2 = Lambda.with(List.of(arg2),
            block(
                call(function, arg2),
                call(function, arg2)));
        Closure runnable1 = FunctionTranslator.translate(function);
        Closure runnable2 = FunctionTranslator.translate(function2);
        for (int i = 0; i < 3; i++) {
            runnable2.invoke("hello");
        }
        for (int i = 0; i < 4; i++) {
            runnable2.invoke(42);
        }
        assertEquals(3, runnable2.implementation.parameters().get(0).profile.referenceCases());
        assertEquals(4, runnable2.implementation.parameters().get(0).profile.intCases());
        assertEquals(6, runnable1.implementation.parameters().get(0).profile.referenceCases());
        assertEquals(8, runnable1.implementation.parameters().get(0).profile.intCases());
    }

    @Test
    public void testLetVarProfile() {
        Variable arg = var("arg");
        Variable t = var("t");
        Closure function = FunctionTranslator.translate(
            Lambda.with(List.of(arg),
                let(t, arg, t)));
        for (int i = 0; i < 3; i++) {
            function.invoke("hello");
        }
        for (int i = 0; i < 4; i++) {
            function.invoke(42);
        }
        LetNode let = (LetNode) function.implementation.body();
        VariableDefinition tDef = let.variable();
        assertEquals(3, tDef.profile.referenceCases());
        assertEquals(4, tDef.profile.intCases());
    }
}
