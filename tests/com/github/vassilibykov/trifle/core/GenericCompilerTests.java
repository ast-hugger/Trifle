// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.expression.Expression;
import com.github.vassilibykov.trifle.expression.Lambda;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;

/**
 * Tests of compiling into generic code.
 */
public class GenericCompilerTests extends LanguageFeaturesTest {

    @Override
    protected Object eval(Expression expression) {
        var function = UserFunction.construct("test", lambda(() -> expression));
        function.implementation().forceCompile();
        return function.invoke();
    }

    @Override
    protected Object invoke(Lambda definition, Object... args) {
        var function = UserFunction.construct("test", definition);
        function.implementation().forceCompile();
        Object result = function.invokeWithArguments(args);
        return result;
    }
}
