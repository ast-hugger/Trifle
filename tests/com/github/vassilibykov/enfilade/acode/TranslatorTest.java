// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.Function;
import com.github.vassilibykov.enfilade.core.Variable;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.add;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.let;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.nullaryFunction;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.prog;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.ref;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.set;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.unaryFunction;
import static com.github.vassilibykov.enfilade.core.ExpressionLanguage.var;
import static org.junit.Assert.*;

public class TranslatorTest {

    @Test
    public void testConstant() {
        Function function = nullaryFunction(() -> const_(42));
        assertEquals(42, interpretAsACode(function));
    }

    @Test
    public void testIf() {
        Function function = unaryFunction(
            arg ->
                if_(ref(arg),
                    const_("true"),
                    const_("false")));
        assertEquals("true", interpretAsACode(function, true));
        assertEquals("false", interpretAsACode(function, false));
    }

    @Test
    public void testLetAndPrimitive() {
        Variable t = var("t");
        Variable u = var("u");
        Function function = nullaryFunction(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                        add(ref(t), ref(u)))));
        assertEquals(7, interpretAsACode(function));
    }

    @Test
    public void testSetVar() {
        Function function = unaryFunction(
            arg ->
                prog(
                    set(arg, const_(42)),
                    ref(arg)));
        assertEquals(42, interpretAsACode(function, 3));
    }

    private Object interpretAsACode(Function function, Object... args) {
        Instruction[] code = Translator.translate(function.body());
        Object[] locals = new Object[function.localsCount()];
        System.arraycopy(args, 0, locals, 0, args.length);
        Interpreter interpreter = Interpreter.on(code, locals);
        return interpreter.interpret();
    }
}