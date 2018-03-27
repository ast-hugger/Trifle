// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import com.github.vassilibykov.enfilade.expression.Variable;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.let;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.nullaryFunction;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.prog;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.set;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.unaryFunction;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.var;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.add;
import static org.junit.Assert.*;

public class TranslatorTest {

    @Test
    public void testConstant() {
        Lambda function = nullaryFunction(() -> const_(42));
        assertEquals(42, interpretAsACode(function));
    }

    @Test
    public void testIf() {
        Lambda function = unaryFunction(
            arg ->
                if_(arg,
                    const_("true"),
                    const_("false")));
        assertEquals("true", interpretAsACode(function, true));
        assertEquals("false", interpretAsACode(function, false));
    }

    @Test
    public void testLetAndPrimitive() {
        Variable t = var("t");
        Variable u = var("u");
        Lambda function = nullaryFunction(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                        add(t, u))));
        assertEquals(7, interpretAsACode(function));
    }

    @Test
    public void testSetVar() {
        Lambda function = unaryFunction(
            arg ->
                prog(
                    set(arg, const_(42)),
                    arg));
        assertEquals(42, interpretAsACode(function, 3));
    }

    private Object interpretAsACode(Lambda function, Object... args) {
        RuntimeFunction runtimeFunction =
            FunctionTranslator.translate(function);
        ACodeInstruction[] code = ACodeTranslator.translate(runtimeFunction.body());
        Object[] locals = new Object[runtimeFunction.localsCount()];
        System.arraycopy(args, 0, locals, 0, args.length);
        ACodeInterpreter interpreter = ACodeInterpreter.on(code, locals);
        return interpreter.interpret();
    }
}