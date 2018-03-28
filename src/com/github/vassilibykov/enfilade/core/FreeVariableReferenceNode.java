// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * A reference from a nested function to a variable defined in an outer function.
 */
class FreeVariableReferenceNode extends VariableReferenceNode {
    private final int frameIndex;

    FreeVariableReferenceNode(@NotNull VariableDefinition variable, int frameIndex) {
        super(variable);
        this.frameIndex = frameIndex;
    }

    public int frameIndex() {
        return frameIndex;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFreeVarReference(this);
    }
}
