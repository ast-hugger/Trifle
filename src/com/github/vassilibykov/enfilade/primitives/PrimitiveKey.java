// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import com.github.vassilibykov.enfilade.core.Primitive1Node;
import com.github.vassilibykov.enfilade.core.Primitive2Node;
import com.github.vassilibykov.enfilade.expression.PrimitiveCallTarget;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

class PrimitiveKey implements PrimitiveCallTarget {
    private final String name;
    private final Function<EvaluatorNode, Primitive1Node> arity1Factory;
    private final BiFunction<EvaluatorNode, EvaluatorNode, Primitive2Node> arity2Factory;

    PrimitiveKey(@NotNull String name, @NotNull Function<EvaluatorNode, Primitive1Node> nodeMaker) {
        this.name = name;
        this.arity1Factory = nodeMaker;
        this.arity2Factory = null;
    }

    PrimitiveKey(@NotNull String name, @NotNull BiFunction<EvaluatorNode, EvaluatorNode, Primitive2Node> nodeMaker) {
        this.name = name;
        this.arity1Factory = null;
        this.arity2Factory = nodeMaker;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public EvaluatorNode link(List<EvaluatorNode> args) {
        switch (args.size()) {
            case 1:
                if (arity1Factory == null) {
                    throw new CompilerError("incorrect number of arguments");
                }
                return arity1Factory.apply(args.get(0));
            case 2:
                if (arity2Factory == null) {
                    throw new CompilerError("incorrect number of arguments");
                }
                return arity2Factory.apply(args.get(0), args.get(1));
            default:
                throw new UnsupportedOperationException("not yet implemented");
        }
    }
}
