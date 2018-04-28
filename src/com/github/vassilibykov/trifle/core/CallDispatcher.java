// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.util.Optional;

/**
 * An object associated with a {@link CallNode}, defining the binding policy of
 * the call, that is, the mapping between the call and the actual executable
 * code that gets invoked.
 *
 * <p>Indirection introduced this way serves two purposes. One, it allows more
 * optimal linking and invocation strategies for certain calls. For example, a
 * call whose target is a constant reference to a built-in function can be
 * performed more cheaply than a call whose target is an arbitrary expression.
 * Second, together with the {@link
 * com.github.vassilibykov.trifle.expression.Callable} interface it allows
 * calls with pluggable late binding strategies, such as calls with Smalltalk
 * message send semantics.
 */
public interface CallDispatcher {
    Optional<EvaluatorNode> evaluatorNode();

    /**
     * Perform a call and return the result. This is typically used by the
     * interpreter to evaluate a {@link CallNode}.
     *
     * @param call The call node to which this dispatcher belongs.
     * @param visitor The visitor, typically an interpreter, that requires
     *        the result. It can be asked to evaluate the call arguments
     *        as needed.
     * @return The value returned by the call target.
     */
    Object execute(CallNode call, EvaluatorNode.Visitor<Object> visitor);

    /**
     * Generate executable code executing which results in calling the function.
     */
    Gist generateCode(CallNode call, CodeGenerator generator);
}
