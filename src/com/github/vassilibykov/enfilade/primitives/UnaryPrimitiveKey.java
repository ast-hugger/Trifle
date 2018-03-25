// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import com.github.vassilibykov.enfilade.core.Primitive1Node;
import com.github.vassilibykov.enfilade.expression.AtomicExpression;
import com.github.vassilibykov.enfilade.expression.PrimitiveCallTarget;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

class UnaryPrimitiveKey implements PrimitiveCallTarget {
    @NotNull private final AtomicExpression argument;
    @NotNull private final Function<EvaluatorNode, Primitive1Node> nodeMaker;

    UnaryPrimitiveKey(@NotNull AtomicExpression argument, @NotNull Function<EvaluatorNode, Primitive1Node> nodeMaker) {
        this.argument = argument;
        this.nodeMaker = nodeMaker;
    }

    @Override
    public EvaluatorNode translate(List<EvaluatorNode> args) {
        if (args.size() != 1) {
            throw new CompilerError("incorrect number of arguments");
        }
        return nodeMaker.apply(args.get(0));
    }
}
