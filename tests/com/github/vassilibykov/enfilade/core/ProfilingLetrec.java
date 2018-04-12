// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;
import org.junit.Before;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.letrec;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.set;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.var;

public class ProfilingLetrec extends ProfilingLet {

    @Before
    public void setUp() throws Exception {
        var topLevel = new TopLevel();
        var t = var("t");
        topLevel.define("test",
            lambda((shouldSet, setValue) ->
                letrec(t, const_(1),
                    if_(shouldSet,
                        set(t, setValue),
                        block()))));
        function = topLevel.getAsClosure("test");
        var let = (LetNode) function.implementation.body();
        var tDef = let.variable();
        profile = tDef.profile;
    }
}
