package com.github.vassilibykov.enfilade;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.CodeFactory.*;
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
            interpreter.interpret(caller, new Object[0]);
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
            interpreter.interpret(method2, new Object[]{"hello"});
        }
        for (int i = 0; i < 4; i++) {
            interpreter.interpret(method2, new Object[]{42});
        }
        assertEquals(3, method2.profile.refCaseCount(arg2));
        assertEquals(4, method2.profile.intCaseCount(arg2));
        assertEquals(6, method.profile.refCaseCount(arg));
        assertEquals(8, method.profile.intCaseCount(arg));
    }

    @Test
    public void testLetVarProfile() {
        Var arg = var("arg");
        Var t = var("t");
        Method method = Method.with(new Var[]{arg},
            let(t, arg, t));
        for (int i = 0; i < 3; i++) {
            interpreter.interpret(method, new Object[]{"hello"});
        }
        for (int i = 0; i < 4; i++) {
            interpreter.interpret(method, new Object[]{42});
        }
        assertEquals(3, method.profile.refCaseCount(t));
        assertEquals(4, method.profile.intCaseCount(t));
    }
}
