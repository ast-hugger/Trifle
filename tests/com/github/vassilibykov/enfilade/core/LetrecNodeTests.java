// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.letrec;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.var;

public class LetrecNodeTests extends LetNodeTests {
    @Override
    protected void defineLetFunction(TopLevel topLevel) {
        var t = var("t");
        topLevel.define("let",
            lambda(f ->
                letrec(t, call(f),
                    t)));
    }
}
