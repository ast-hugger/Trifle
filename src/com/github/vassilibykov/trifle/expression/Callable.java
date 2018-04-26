// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.EvaluatorNode;

/**
 * An object that can appear as the target of a {@link Call} expression.
 *
 * <p>All atomic expressions are callable, so an atomic expression may be
 * used as a target, with an expectation that it will evaluate to a
 * {@link com.github.vassilibykov.trifle.core.Invocable}. An important
 * special case are {@link FreeFunctionReference}s, which are atomic
 * expressions recognized by the compiler to produce a more efficient
 * call sequence than the general case.
 *
 * <p>A callable, however, does not have to be an expression. This more general
 * case allows to support alternative call binding schemes. For example, a
 * Smalltalk style message send would be expressed as a call with a special
 * callable as the target, incorporating the selector and choosing a different
 * logic of selecting the function to invoke at the time of the call.
 */
public interface Callable {
    /**
     * Used in the translation phase into evaluator node tree. Produces the call
     * dispatcher that will be used by the containing call expression.
     */
    CallDispatcher createDispatcher(Visitor<EvaluatorNode> translator);
}
