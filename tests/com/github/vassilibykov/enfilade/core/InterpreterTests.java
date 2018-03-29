// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Expression;
import com.github.vassilibykov.enfilade.expression.ExpressionLanguage;
import com.github.vassilibykov.enfilade.expression.Lambda;
import com.github.vassilibykov.enfilade.expression.Variable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.*;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("Convert2MethodRef")
public class InterpreterTests extends LanguageFeaturesTest {

    @Override
    protected Object eval(Expression methodBody) {
        Lambda function = Lambda.with(List.of(), methodBody);
        return invoke(function);
    }

    @Override
    protected Object invoke(Lambda function, Object... args) {
        switch (args.length) {
            case 0:
                return FunctionTranslator.translate(function).invoke();
            case 1:
                return FunctionTranslator.translate(function).invoke(args[0]);
            case 2:
                return FunctionTranslator.translate(function).invoke(args[0], args[1]);
            default:
            throw new IllegalArgumentException();
        }
    }

    @Ignore
    @Test
    public void testEvilFibonacci() {
        Lambda fibonacci = evilFibonacci();
        invoke(fibonacci, 35); // enough to force compilation
        assertEquals(1, invoke(fibonacci, 0));
        assertEquals(8, invoke(fibonacci, 5));
        assertEquals("error", invoke(fibonacci, -1));
    }

    @Ignore
    @Test
    public void testVeryEvilFibonacci() {
        Lambda fibonacci = veryEvilFibonacci();
        invoke(fibonacci, 35); // enough to force compilation
        assertEquals(1, invoke(fibonacci, 0));
        assertEquals(8, invoke(fibonacci, 5));
        assertEquals("error", invoke(fibonacci, -1));
    }

    /*
        Support
     */

    /**
     * A version of fibonacci which profiles with all the same types as the
     * regular one, but includes an 'if' branch (not taken while being profiled)
     * which injects a value into computation incompatible with the profiled
     * types, and therefore with the specialized version of code.
     */
    static Lambda evilFibonacci() {
        Variable n = var("n");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Lambda.recursive(n, fibonacci ->
            if_(lessThan(n, const_(0)),
                const_("error"),
                if_(lessThan(n, const_(2)),
                    const_(1),
                    let(t1, call(fibonacci, sub(n, const_(1))),
                        let(t2, call(fibonacci, sub(n, const_(2))),
                            add(t1, t2))))));
    }

    /**
     * Here, unlike the simply evil version, the "error" constant is not in the
     * tail position, so the continuation which should but can't receive its
     * value lies within the function.
     */
    static Lambda veryEvilFibonacci() {
        Variable n = var("n");
        Variable t0 = var("t0");
        Variable t1 = var("t1");
        Variable t2 = var("t2");
        return Lambda.recursive(n, fibonacci ->
            let(t0, if_(lessThan(n, const_(0)),
                        const_("error"),
                        if_(lessThan(n, const_(2)),
                            const_(1),
                            let(t1, call(fibonacci, sub(n, const_(1))),
                                let(t2, call(fibonacci, sub(n, const_(2))),
                                    add(t1, t2))))),
                t0));
    }
}