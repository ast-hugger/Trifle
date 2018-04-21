// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Expression;
import com.github.vassilibykov.enfilade.expression.Lambda;
import org.junit.Before;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;

/**
 * Tests of compiling into generic code.
 */
public class GenericCompilerTests extends LanguageFeaturesTest {

    private TopLevel topLevel;

    @Before
    public void setUp() throws Exception {
        topLevel = new TopLevel();
    }

    @Override
    protected Object eval(Expression expression) {
        topLevel.define("test", lambda(() -> expression));
        var function = topLevel.getAsClosure("test");
        function.implementation.forceCompile();
        return function.invoke();
    }

    @Override
    protected Object invoke(Lambda definition, Object... args) {
        topLevel.define("test", definition);
        var function = topLevel.getAsClosure("test");
        function.implementation.forceCompile();
        return function.invoke(args);
    }
}
