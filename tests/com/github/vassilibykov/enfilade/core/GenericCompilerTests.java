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
