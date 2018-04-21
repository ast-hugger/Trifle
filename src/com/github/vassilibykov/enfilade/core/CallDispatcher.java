// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.Optional;

/**
 * An object associated with a {@link CallNode} which defines the mapping
 * between the call and the actual executable code invoked.
 *
 * <p>Indirection introduced this way serves two purposes. One, it allows more
 * optimal linking invocation strategies for certain calls. For example, a call
 * whose target is a constant reference to a built-in function can be performed
 * more cheaply than a call whose target is an arbitrary expression. Second,
 * together with the {@link com.github.vassilibykov.enfilade.expression.Callable}
 * interface it allows calls with pluggable late binding strategies, such as
 * calls with Smalltalk message send semantics.
 */
public interface CallDispatcher {
    Optional<EvaluatorNode> evaluatorNode();

    /**
     * Produce an {@link Invocable} invoking which with call argument
     * values will execute the call. This is part of the interpreter
     * evaluation path.
     */
    Invocable getInvocable(EvaluatorNode.Visitor<Object> visitor);

    /**
     * Generate executable code which will result in calling the function.
     */
    JvmType generateCode(CallNode call, CodeGenerator generator);
}
