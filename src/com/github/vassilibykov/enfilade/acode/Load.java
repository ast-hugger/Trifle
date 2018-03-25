// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluate the atomic expression and set the value register to contain the result.
 */
public class Load extends Instruction {
    @NotNull /*internal*/ final EvaluatorNode expression;

    Load(@NotNull EvaluatorNode expression) {
        this.expression = expression;
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitLoad(this);
    }

    @Override
    public String toString() {
        return "LOAD " + expression;
    }
}
