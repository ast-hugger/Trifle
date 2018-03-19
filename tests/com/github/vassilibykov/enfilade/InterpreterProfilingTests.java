package com.github.vassilibykov.enfilade;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.ExpressionLanguage.*;
import static org.junit.Assert.assertEquals;

public class InterpreterProfilingTests {

    private static Interpreter interpreter;

    @BeforeClass
    public static void setUpClass() {
        interpreter = new Interpreter();
    }

    @Test
    public void testInvocationsCount() {
        Method callee = Method.with(new Var[0], const_(42));
        Method caller = Method.with(new Var[0],
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
        Var arg = var("arg");
        Method method = Method.with(new Var[]{arg}, arg);
        Var arg2 = var("arg2");
        Method method2 = Method.with(new Var[]{arg2},
            prog(
                call(method, arg2),
                call(method, arg2)));
        for (int i = 0; i < 3; i++) {
            method2.invoke("hello");
        }
        for (int i = 0; i < 4; i++) {
            method2.invoke(42);
        }
        assertEquals(3, arg2.profile.referenceCases());
        assertEquals(4, arg2.profile.intCases());
        assertEquals(6, arg.profile.referenceCases());
        assertEquals(8, arg.profile.intCases());
    }

    @Test
    public void testLetVarProfile() {
        Var arg = var("arg");
        Var t = var("t");
        Method method = Method.with(new Var[]{arg},
            let(t, arg, t));
        for (int i = 0; i < 3; i++) {
            method.invoke("hello");
        }
        for (int i = 0; i < 4; i++) {
            method.invoke(42);
        }
        assertEquals(3, t.profile.referenceCases());
        assertEquals(4, t.profile.intCases());
    }
}
