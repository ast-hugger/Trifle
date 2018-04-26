// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.core.ExpressionCallDispatcher;

public abstract class AtomicExpression extends Expression implements Callable {
    @Override
    public CallDispatcher createDispatcher(Visitor<EvaluatorNode> translator) {
        return new ExpressionCallDispatcher(this.accept(translator));
    }
}
