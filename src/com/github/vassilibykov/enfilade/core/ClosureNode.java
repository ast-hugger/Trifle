// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import org.jetbrains.annotations.NotNull;

class ClosureNode extends EvaluatorNode {
    @NotNull private final Lambda definition;
    @NotNull private final FunctionImplementation function;

    ClosureNode(@NotNull Lambda definition, @NotNull FunctionImplementation function) {
        this.definition = definition;
        this.function = function;
    }

    public Lambda definition() {
        return definition;
    }

    public FunctionImplementation function() {
        return function;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitClosure(this);
    }
}
