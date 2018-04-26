// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import com.github.vassilibykov.trifle.expression.FreeFunctionReference;

import java.lang.invoke.MethodType;

import static com.github.vassilibykov.trifle.core.JvmType.REFERENCE;

/**
 * A node representing a reference to a built-in of user-defined function
 * introduced in the input using the {@link FreeFunctionReference}
 * expression.
 */
class FreeFunctionReferenceNode extends EvaluatorNode {
    private final FreeFunction target;

    FreeFunctionReferenceNode(FreeFunction target) {
        this.target = target;
    }

    public FreeFunction target() {
        return target;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFreeFunctionReference(this);
    }

    /**
     * Using the supplied writer, generate code that results in the target of
     * the reference being loaded on the stack. This code is here to avoid
     * duplication across all the code generators which all handle this the same
     * way.
     */
    JvmType generateLoad(GhostWriter writer) {
        if (target instanceof BuiltinFunction) {
            writer.invokeDynamic(
                BuiltinFunctionReferenceInvokeDynamic.BOOTSTRAP,
                ((BuiltinFunction) target).name(),
                MethodType.methodType(Object.class));
        } else if (target instanceof UserFunction) {
            writer.invokeDynamic(
                UserFunctionReferenceInvokeDynamic.BOOTSTRAP,
                "userFunctionRef",
                MethodType.methodType(Object.class),
                ((UserFunction) target).implementation().id());
        } else {
            throw new AssertionError("unexpected target: " + target);
        }
        return REFERENCE;
    }
}
