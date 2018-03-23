// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.AtomicExpression;

/**
 * Set the instruction pointer to the specified address if the test evaluates to
 * true.
 */
public class Branch extends Instruction {
    /*internal*/ final AtomicExpression test;
    /*internal*/ int address;

    Branch(AtomicExpression test, int address) {
        this.test = test;
        this.address = address;
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitBranch(this);
    }
}
