// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.Objects;

/**
 * A node which evaluates to a closure, much like a regular closure node.
 * However, unlike a regular closure node, the "closure" it evaluates to is one
 * for a top-level function (so no copied values) which never changes. Thus when
 * it appears as the function subexpression of a call expression, the call is
 * compiled as a special kind of invokedynamic which links to the target
 * function much more efficiently.
 */
class FunctionConstantNode extends EvaluatorNode {
    private final int functionId;

    FunctionConstantNode(FunctionImplementation function) {
        if (!function.isTopLevel()) {
            throw new IllegalArgumentException();
        }
        functionId = FunctionRegistry.INSTANCE.lookup(function);
    }

    FunctionImplementation function() {
        return Objects.requireNonNull(FunctionRegistry.INSTANCE.lookup(functionId));
    }

    int id() {
        return functionId;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitConstantFunction(this);
    }
}
