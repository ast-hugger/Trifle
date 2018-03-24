// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

/**
 * Unconditionally set the instruction pointer to the specified address.
 */
public class Goto extends Instruction {
    /*internal*/ int address;

    Goto(int address) {
        this.address = address;
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitGoto(this);
    }

    @Override
    public String toString() {
        return "GOTO " + address;
    }
}
