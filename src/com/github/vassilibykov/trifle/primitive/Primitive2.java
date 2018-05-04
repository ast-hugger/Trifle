// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.expression.Primitive;

/**
 * The abstract superclass of binary primitive implementations.
 */
public abstract class Primitive2 implements Primitive {

    /**
     * Infer the type of the value returned by the operation.
     */
    public abstract ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type);

    /**
     * Perform the primitive operation on the given argument values.
     * This method is used by the interpreter.
     */
    public abstract Object apply(Object argument1, Object argument2);

    /**
     * Generate code to perform the operation when both arguments on the stack
     * are of a reference type.
     */
    protected abstract JvmType generateForReferenceReference(GhostWriter writer);

    /**
     * Generate code to perform the operation when the first argument is a
     * reference and the second one is an {@code int}. Both arguments are
     * already on the stack.
     */
    protected abstract JvmType generateForReferenceInt(GhostWriter writer);

    /**
     * Generate code for the {@code (reference, boolean} argument combination.
     */
    protected abstract JvmType generateForReferenceBoolean(GhostWriter writer);

    /**
     * Generate code for the {@code (int, reference} argument combination.
     */
    protected abstract JvmType generateForIntReference(GhostWriter writer);

    /**
     * Generate code for the {@code (int, int} argument combination.
     */
    protected abstract JvmType generateForIntInt(GhostWriter writer);

    /**
     * Generate code for the {@code (int, boolean} argument combination.
     */
    protected abstract JvmType generateForIntBoolean(GhostWriter writer);

    /**
     * Generate code for the {@code (boolean, reference} argument combination.
     */
    protected abstract JvmType generateForBooleanReference(GhostWriter writer);

    /**
     * Generate code for the {@code (boolean, int} argument combination.
     */
    protected abstract JvmType generateForBooleanInt(GhostWriter writer);

    /**
     * Generate code for the {@code (boolean, boolean} argument combination.
     */
    protected abstract JvmType generateForBooleanBoolean(GhostWriter writer);

    /**
     * Generate code for the specified argument type combination. This method
     * should not be overridden. Instead, a subclass should implement the ones
     * for the specific type cases.
     */
    public final JvmType generate(GhostWriter writer, JvmType arg1type, JvmType arg2type) {
        return arg1type.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                return arg2type.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (Object, Object)
                        return generateForReferenceReference(writer);
                    }

                    public JvmType ifInt() { // (Object, int)
                        return generateForReferenceInt(writer);
                    }

                    public JvmType ifBoolean() { // (Object, boolean)
                        return generateForReferenceBoolean(writer);
                    }
                });
            }

            public JvmType ifInt() {
                return arg2type.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (int, Object)
                        return generateForIntReference(writer);
                    }

                    public JvmType ifInt() { // (int, int)
                        return generateForIntInt(writer);
                    }

                    public JvmType ifBoolean() {
                        return generateForIntBoolean(writer);
                    }
                });
            }

            public JvmType ifBoolean() {
                return arg2type.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (int, Object)
                        return generateForBooleanReference(writer);
                    }

                    public JvmType ifInt() { // (int, int)
                        return generateForBooleanInt(writer);
                    }

                    public JvmType ifBoolean() {
                        return generateForBooleanBoolean(writer);
                    }
                });
            }
        });
    }
}
