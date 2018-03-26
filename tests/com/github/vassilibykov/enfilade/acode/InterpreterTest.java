// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.CallNode;
import com.github.vassilibykov.enfilade.core.FunctionTranslator;
import com.github.vassilibykov.enfilade.core.Primitive1Node;
import com.github.vassilibykov.enfilade.core.RuntimeFunction;
import com.github.vassilibykov.enfilade.core.VariableDefinition;
import com.github.vassilibykov.enfilade.expression.ExpressionLanguage;
import com.github.vassilibykov.enfilade.core.ExpressionTestUtilities;
import com.github.vassilibykov.enfilade.expression.Function;
import com.github.vassilibykov.enfilade.primitives.Add;
import com.github.vassilibykov.enfilade.primitives.LessThan;
import com.github.vassilibykov.enfilade.primitives.Negate;
import com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.acode.AssemblyLanguage.*;
import static com.github.vassilibykov.enfilade.core.ExpressionTestUtilities.varDef;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.binaryFunction;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.add;
import static org.junit.Assert.assertEquals;

public class InterpreterTest {

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
        VariableDefinition arg = varDef("arg");
        code = Asm
            .vars(arg)
            .code(
                load(arg),
                ret());
        assertEquals(42, code.interpretWith(42));
    }

    @Test
    public void testPrimitive1() {
        VariableDefinition arg = varDef("arg");
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
        VariableDefinition arg = varDef("arg");
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
        VariableDefinition arg = varDef("arg");
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
        VariableDefinition arg1 = varDef("arg1");
        RuntimeFunction adder = FunctionTranslator.translate(
            binaryFunction((a, b) -> add(a, b)));
        // In general variables must not be reused, but reuse here is ok because we know
        // the are in same positions in the frame and the code is executed only once so it's not being compiled.
        code = Asm
            .vars(arg1)
            .code(
                call(ExpressionTestUtilities.call(adder, ref(arg1), ref(arg1))),
                ret());
        assertEquals(6, code.interpretWith(3));
    }

    /** A helper for defining test methods. */
    private static class Asm {

        static Asm vars(VariableDefinition... vars) {
            return new Asm(vars);
        }

        private final Object[] frame;
        private Instruction[] instructions;

        private Asm(VariableDefinition... vars) {
            for (int i = 0; i < vars.length; i++) {
                ExpressionTestUtilities.setVariableIndex(vars[i], i);
            }
            frame = new Object[vars.length];
        }

        Asm code(Instruction... instructions) {
            this.instructions = instructions;
            return this;
        }

        Object interpretWith(Object... args) {
            System.arraycopy(args, 0, frame, 0, args.length);
            return new Interpreter(instructions, frame, 0).interpret();
        }
    }
}