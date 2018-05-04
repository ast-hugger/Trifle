// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.expression.Primitive;

/**
 * The abstract superclass of unary primitives implementations.
 */
public abstract class Primitive1 implements Primitive {

    /**
     * Infer the type of the value returned by the operation.
     */
    public abstract ExpressionType inferredType(ExpressionType argumentType);

    /**
     * Perform the primitive operation on the given argument value.
     * This method is used by the interpreter.
     */
    public abstract Object apply(Object argument);

    /**
     * Generate code to perform the operation when the argument (already
     * on the stack) is a reference.
     */
    protected abstract JvmType generateForReference(GhostWriter writer);

    /**
     * Generate code to perform the operation when the argument (already
     * on the stack) is an {@code int}.
     */
    protected abstract JvmType generateForInt(GhostWriter writer);

    /**
     * Generate code to perform the operation when the argument (already
     * on the stack) is a {@code boolean}.
     */
    protected abstract JvmType generateForBoolean(GhostWriter writer);

    /**
     * Generate code to perform the operation when the argument on the
     * stack is of the specified type. Instead of overriding this method,
     * a subclass should implement the ones for the specific type cases.
     */
    public final JvmType generate(GhostWriter writer, JvmType argType) {
        return argType.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                return generateForReference(writer);
            }

            public JvmType ifInt() {
                return generateForInt(writer);
            }

            public JvmType ifBoolean() {
                return generateForBoolean(writer);
            }
        });
    }
}
