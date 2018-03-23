// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

/**
 * Return the value of the register as the result of this invocation.
 */
public class Return extends Instruction {
    Return() {
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitReturn(this);
    }
}
