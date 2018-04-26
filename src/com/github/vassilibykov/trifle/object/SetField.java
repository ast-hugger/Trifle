// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.expression.Callable;
import com.github.vassilibykov.trifle.expression.Visitor;

public class SetField implements Callable {

    public static SetField named(String fieldName) {
        return new SetField(fieldName);
    }

    private final String fieldName;

    private SetField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }

    @Override
    public CallDispatcher createDispatcher(Visitor<EvaluatorNode> translator) {
        return new SetFieldDispatcher(fieldName);
    }
}
