// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Expression;
import com.github.vassilibykov.enfilade.expression.Lambda;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;

/**
 * Tests of generated generic code.
 */
public class GenericCompilerTests extends LanguageFeaturesTest {

    @Override
    protected Object eval(Expression expression) {
        var function = FunctionTranslator.translate(lambda(() -> expression));
        function.implementation.forceCompile();
        return function.invoke();
    }

    @Override
    protected Object invoke(Lambda definition, Object... args) {
        var function = FunctionTranslator.translate(definition);
        function.implementation.forceCompile();
        return function.invoke(args);
    }
}
