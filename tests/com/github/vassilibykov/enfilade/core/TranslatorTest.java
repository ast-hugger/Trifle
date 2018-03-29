// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.ExpressionLanguage;
import com.github.vassilibykov.enfilade.expression.Lambda;
import com.github.vassilibykov.enfilade.expression.Variable;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.let;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.set;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.var;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.add;
import static org.junit.Assert.*;

public class TranslatorTest {

    @Test
    public void testConstant() {
        Lambda function = ExpressionLanguage.lambda(() -> const_(42));
        assertEquals(42, interpretAsACode(function));
    }

    @Test
    public void testIf() {
        Lambda function = lambda(
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
        Lambda function = ExpressionLanguage.lambda(
            () ->
                let(t, const_(3),
                    let(u, const_(4),
                        add(t, u))));
        assertEquals(7, interpretAsACode(function));
    }

    @Test
    public void testSetVar() {
        Lambda function = lambda(
            arg ->
                block(
                    set(arg, const_(42)),
                    arg));
        assertEquals(42, interpretAsACode(function, 3));
    }

    private Object interpretAsACode(Lambda lambda, Object... args) {
        Closure function =
            FunctionTranslator.translate(lambda);
        ACodeInstruction[] code = ACodeTranslator.translate(function.implementation.body());
        Object[] locals = new Object[function.implementation.frameSize()];
        System.arraycopy(args, 0, locals, 0, args.length);
        ACodeInterpreter interpreter = ACodeInterpreter.on(code, locals);
        return interpreter.interpret();
    }
}