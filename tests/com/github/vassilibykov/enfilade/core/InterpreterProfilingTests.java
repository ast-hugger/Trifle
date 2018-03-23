// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.*;
import static org.junit.Assert.assertEquals;

public class InterpreterProfilingTests {

    private static Interpreter interpreter;

    @BeforeClass
    public static void setUpClass() {
        interpreter = new Interpreter();
    }

    @Test
    public void testInvocationsCount() {
        Function callee = Function.with(new Variable[0], const_(42));
        Function caller = Function.with(new Variable[0],
            prog(
                call(callee),
                call(callee)));
        for (int i = 0; i < 7; i++) {
            caller.invoke();
        }
        assertEquals(7, caller.profile.invocationCount());
        assertEquals(14, callee.profile.invocationCount());
    }

    @Test
    public void testMethodArgTypeProfile() {
        Variable arg = var("arg");
        Function function = Function.with(new Variable[]{arg}, ref(arg));
        Variable arg2 = var("arg2");
        Function function2 = Function.with(new Variable[]{arg2},
            prog(
                call(function, ref(arg2)),
                call(function, ref(arg2))));
        for (int i = 0; i < 3; i++) {
            function2.invoke("hello");
        }
        for (int i = 0; i < 4; i++) {
            function2.invoke(42);
        }
        assertEquals(3, arg2.profile.referenceCases());
        assertEquals(4, arg2.profile.intCases());
        assertEquals(6, arg.profile.referenceCases());
        assertEquals(8, arg.profile.intCases());
    }

    @Test
    public void testLetVarProfile() {
        Variable arg = var("arg");
        Variable t = var("t");
        Function function = Function.with(new Variable[]{arg},
            let(t, ref(arg), ref(t)));
        for (int i = 0; i < 3; i++) {
            function.invoke("hello");
        }
        for (int i = 0; i < 4; i++) {
            function.invoke(42);
        }
        assertEquals(3, t.profile.referenceCases());
        assertEquals(4, t.profile.intCases());
    }
}
