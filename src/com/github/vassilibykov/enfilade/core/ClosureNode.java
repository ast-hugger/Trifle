// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

class ClosureNode extends EvaluatorNode {
    @TestOnly
    static ClosureNode withNoCopiedValues(FunctionImplementation function) {
        var node = new ClosureNode(function);
        node.indicesToCopy = new int[0];
        return node;
    }

    @NotNull private final FunctionImplementation function;
    /*internal*/ int[] indicesToCopy; // set in a separate phase by the analyzer

    ClosureNode(@NotNull FunctionImplementation function) {
        this.function = function;
    }

    public Lambda definition() {
        return function.definition();
    }

    public FunctionImplementation function() {
        return function;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitClosure(this);
    }
}
