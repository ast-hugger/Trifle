// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import com.github.vassilibykov.enfilade.core.CallDispatcher;
import com.github.vassilibykov.enfilade.core.EvaluatorNode;

/**
 * Something that can appear as the call target of a {@link Call} expression.
 */
public interface Callable {
    /**
     * Used in the translation phase into evaluator node tree. Produces the call
     * dispatcher that will be used by the containing call expression.
     */
    CallDispatcher createDispatcher(Visitor<EvaluatorNode> translator);
}
