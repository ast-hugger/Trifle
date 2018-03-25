// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.EvaluatorNode;

/**
 * Set the instruction pointer to the specified address if the test evaluates to
 * true.
 */
public class Branch extends Instruction {
    /*internal*/ final EvaluatorNode test;
    /*internal*/ int address;

    Branch(EvaluatorNode test, int address) {
        this.test = test;
        this.address = address;
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitBranch(this);
    }

    @Override
    public String toString() {
        return "BRANCH " + test + " " + address;
    }
}
