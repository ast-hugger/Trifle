// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

/**
 * An explicit return (a bypass in the normal chain of continuations).
 */
class ReturnNode extends EvaluatorNode implements RecoverySite {
    @NotNull private final EvaluatorNode value;
    private Label recoverySiteLabel;

    ReturnNode(@NotNull EvaluatorNode value) {
        this.value = value;
    }

    public EvaluatorNode value() {
        return value;
    }

    @Override
    public Label recoverySiteLabel() {
        return recoverySiteLabel;
    }

    public void setRecoverySiteLabel(Label recoverySiteLabel) {
        this.recoverySiteLabel = recoverySiteLabel;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitReturn(this);
    }

    @Override
    public String toString() {
        return "(ret " + value + ")";
    }
}
