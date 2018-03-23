// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.CallExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Perform the call and set the register to contain the result.
 */
public class Call extends Instruction {
    @NotNull /*internal*/ final CallExpression callExpression;

    Call(@NotNull CallExpression callExpression) {
        this.callExpression = callExpression;
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitCall(this);
    }
}