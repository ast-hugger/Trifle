// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevelFunction;
import com.github.vassilibykov.enfilade.expression.FunctionReference;

/**
 * A node representing a reference to a built-in of user-defined function
 * introduced in the input using the {@link FunctionReference}
 * expression.
 */
class DirectFunctionNode extends EvaluatorNode {
    private final TopLevelFunction target;

    DirectFunctionNode(TopLevelFunction target) {
        this.target = target;
    }

    public TopLevelFunction target() {
        return target;
    }

    int id() {
        return target.asCallable().id();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitConstantFunction(this);
    }
}
