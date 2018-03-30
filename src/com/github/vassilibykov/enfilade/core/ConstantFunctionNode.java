// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ConstantFunctionNode extends EvaluatorNode {

    private static final List<Closure> closureRegistry = new ArrayList<>();
    private static final Map<Closure, Integer> closureIDs = new HashMap<>();

    static int lookup(Closure closure) {
        return closureIDs.computeIfAbsent(closure, it -> {
            closureRegistry.add(it);
            return closureRegistry.size() - 1;
        });
    }

    static Closure lookup(int closureId) {
        return closureRegistry.get(closureId);
    }

    private final TopLevel.Binding binding;

    ConstantFunctionNode(TopLevel.Binding binding) {
        this.binding = binding;
    }

    Closure closure() {
        return binding.closure();
    }

    int id() {
        return lookup(binding.closure());
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitConstantFunction(this);
    }
}
