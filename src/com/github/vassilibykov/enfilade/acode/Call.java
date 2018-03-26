// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.CallNode;
import com.github.vassilibykov.enfilade.core.Environment;
import org.jetbrains.annotations.NotNull;

/**
 * Perform the call and set the register to contain the result.
 */
public class Call extends Instruction {
    @NotNull /*internal*/ final CallNode callExpression;

    Call(@NotNull CallNode callExpression) {
        this.callExpression = callExpression;
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitCall(this);
    }

    @Override
    public String toString() {
        return "CALL #" + Environment.INSTANCE.lookup(callExpression.function());
    }
}
