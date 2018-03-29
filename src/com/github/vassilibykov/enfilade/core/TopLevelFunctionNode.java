// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;

public class TopLevelFunctionNode extends EvaluatorNode {
    /*internal*/ final TopLevel.Binding binding;

    TopLevelFunctionNode(TopLevel.Binding binding) {
        this.binding = binding;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitTopLevelFunction(this);
    }
}
