// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Expression;
import com.github.vassilibykov.enfilade.expression.Lambda;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;

/**
 * Tests of compiling into specialized code.
 */
public class SpecializedCompilerTests extends LanguageFeaturesTest {

    @Override
    protected Object eval(Expression expression) {
        var function = Closure.with(FunctionTranslator.translate(lambda(() -> expression)));
        function.invoke();
        function.implementation.forceCompile();
        return function.invoke();
    }

    @Override
    protected Object invoke(Lambda definition, Object... args) {
        var function = Closure.with(FunctionTranslator.translate(definition));
        if (!function.implementation.isCompiled()) {
            function.invoke(args); // to profile
            function.implementation.forceCompile();
        }
        return function.invoke(args);
    }
}
