// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * Store the value of the register in the specified local variable.
 */
public class Store extends Instruction {
    @NotNull /*internal*/ final Variable variable;

    Store(@NotNull Variable variable) {
        this.variable = variable;
    }

    @Override
    public void accept(VoidVisitor visitor) {
        visitor.visitStore(this);
    }
}
