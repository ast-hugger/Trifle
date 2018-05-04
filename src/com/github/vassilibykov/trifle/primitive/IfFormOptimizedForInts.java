// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.core.Primitive2Node;
import org.objectweb.asm.Opcodes;

import java.util.function.Consumer;

class IfFormOptimizedForInts implements IfAware.OptimizedIfForm {
    private Primitive2Node primitiveCall;
    private int opcode;

    /**
     * Create a new instance.
     *
     * @param primitiveCall The node serving as the {@code if} condition.
     * @param opcode The {@code IF_ICMP...} opcode to jump AROUND the true clause.
     *        Note that it is a negation of the comparison operation being implemented.
     */
    IfFormOptimizedForInts(Primitive2Node primitiveCall, int opcode) {
        this.primitiveCall = primitiveCall;
        this.opcode = opcode;
    }

    @Override
    public void loadArguments(Consumer<EvaluatorNode> argumentGenerator) {
        argumentGenerator.accept(primitiveCall.argument1());
        argumentGenerator.accept(primitiveCall.argument2());
    }

    @Override
    public int jumpInstruction() {
        return opcode;
    }
}
