// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class SetFreeVariableNode extends SetVariableNode {
    private final int frameIndex;

    SetFreeVariableNode(@NotNull VariableDefinition variable, int frameIndex, @NotNull EvaluatorNode value) {
        super(variable, value);
        this.frameIndex = frameIndex;
    }

    public int frameIndex() {
        return frameIndex;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSetFreeVar(this);
    }
}
