// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Const;
import com.github.vassilibykov.enfilade.expression.Lambda;
import com.github.vassilibykov.enfilade.expression.Variable;
import com.github.vassilibykov.enfilade.primitives.Add;
import com.github.vassilibykov.enfilade.primitives.LessThan;
import com.github.vassilibykov.enfilade.primitives.Negate;
import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.enfilade.core.AssemblyLanguage.*;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.add;
import static org.junit.Assert.assertEquals;

public class RecoveryInterpreterTest {

    private Asm code;

    @Test
    public void testLoadConst() {
        code = Asm
            .vars()
            .code(
                load("hello"),
                ret());
        assertEquals("hello", code.interpretWith());
    }

    @Test
    public void testLoadArg() {
        VariableDefinition arg = new VariableDefinition(Variable.named("arg"), bogusRuntimeFunction());
        code = Asm
            .vars(arg)
            .code(
                load(arg),
                ret());
        assertEquals(42, code.interpretWith(42));
    }

    @Test
    public void testPrimitive1() {
        VariableDefinition arg = new VariableDefinition(Variable.named("arg"), bogusRuntimeFunction());
        code = Asm
            .vars(arg)
            .code(
                load(new Negate(ref(arg))),
                ret());
        assertEquals(-42, code.interpretWith(42));
    }

    @Test
    public void testPrimitive2() {
        code = Asm
            .vars()
            .code(
                load(new Add(const_(3), const_(4))),
                ret());
        assertEquals(7, code.interpretWith());
    }

    @Test
    public void testGoto() {
        code = Asm
            .vars()
            .code(
                jump(3),
                load("hello"),
                ret(),
                load("goodbye"),
                ret());
        assertEquals("goodbye", code.interpretWith());
    }

    @Test
    public void testIf() {
        VariableDefinition arg = new VariableDefinition(Variable.named("arg"), bogusRuntimeFunction());
        code = Asm
            .vars(arg)
            .code(
                jump(new LessThan(ref(arg), const_(0)), 3),
                load("positive"),
                jump(4),
                load("negative"),
                ret());
        assertEquals("positive", code.interpretWith(1));
        assertEquals("negative", code.interpretWith(-1));
    }

    @Test
    public void testStore() {
        VariableDefinition arg = new VariableDefinition(Variable.named("arg"), bogusRuntimeFunction());
        code = Asm
            .vars(arg)
            .code(
                load(4),
                store(arg),
                load(arg),
                ret());
        assertEquals(4, code.interpretWith(3));
    }

    @Test
    public void testCall() {
        Closure adder = FunctionTranslator.translate(lambda((a, b) -> add(a, b)));
        VariableDefinition arg = new VariableDefinition(Variable.named("arg"), bogusRuntimeFunction());
        code = Asm
            .vars(arg)
            .code(
                call(new CallNode.Call2(const_(adder), ref(arg), ref(arg))),
                ret());
        assertEquals(6, code.interpretWith(3));
    }

    @Test
    public void testLoadAndCallClosure() {
        var adder = FunctionTranslator.translate(lambda((a, b) -> add(a, b)));
        var arg = new VariableDefinition(Variable.named("arg"), bogusRuntimeFunction());
        var temp = new VariableDefinition(Variable.named("temp"), bogusRuntimeFunction());
        code = Asm
            .vars(arg, temp)
            .code(
                load(adder),
                store(temp),
                call(new CallNode.Call2(new GetVariableNode(temp), ref(arg), ref(arg))),
                ret());
        assertEquals(6, code.interpretWith(3));
    }

    /** A helper for defining test methods. */
    private static class Asm {

        static Asm vars(VariableDefinition... vars) {
            return new Asm(vars);
        }

        private final Object[] frame;
        private ACodeInstruction[] instructions;

        private Asm(VariableDefinition... vars) {
            for (int i = 0; i < vars.length; i++) {
                vars[i].index = i;
            }
            frame = new Object[vars.length];
        }

        Asm code(ACodeInstruction... instructions) {
            this.instructions = instructions;
            return this;
        }

        Object interpretWith(Object... args) {
            System.arraycopy(args, 0, frame, 0, args.length);
            return new ACodeInterpreter(instructions, frame, 0).interpret();
        }
    }

    private FunctionImplementation bogusRuntimeFunction() {
        return new FunctionImplementation(Lambda.with(List.of(), Const.value(null)));
    }
}