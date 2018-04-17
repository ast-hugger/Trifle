// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A node evaluating which creates a closure.
 */
class ClosureNode extends EvaluatorNode {

    /**
     * The function implementing this closure.
     */
    @NotNull private final FunctionImplementation function;
    /**
     * A list of variables belonging to the implementation function of the parent closure
     * of this closure which are passed into this closure as synthetic parameters.
     * Set in a separated phase by {@link FunctionAnalyzer.Indexer}.
     */
    /*internal*/ List<AbstractVariable> copiedOuterVariables;
    /**
     * A list of indices of {@link #copiedOuterVariables}. When a closure is created, local
     * variables of the outer function's activation frame with these indices are copied
     * as the closure's {@link Closure#copiedValues}. Set in a separate phase by
     * {@link FunctionAnalyzer.Indexer}.
     */
    /*internal*/ int[] copiedVariableIndices;

    ClosureNode(@NotNull FunctionImplementation function) {
        this.function = function;
    }

    public Lambda definition() {
        return function.definition();
    }

    FunctionImplementation function() {
        return function;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitClosure(this);
    }
}
