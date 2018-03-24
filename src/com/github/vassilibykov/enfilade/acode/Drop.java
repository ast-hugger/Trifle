// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

/**
 * Discard the current value of the register. If the value register is
 * implemented as a register and not a 1-value stack, this is essentially a
 * no-op.
 */
public class Drop extends Instruction {
    Drop() {
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitDrop(this);
    }

    @Override
    public String toString() {
        return "DROP";
    }
}
