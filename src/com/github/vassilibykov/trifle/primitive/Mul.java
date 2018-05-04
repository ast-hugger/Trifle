// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.RuntimeError;

import static com.github.vassilibykov.trifle.core.JvmType.INT;
import static com.github.vassilibykov.trifle.core.JvmType.REFERENCE;
import static org.objectweb.asm.Opcodes.IMUL;

/**
 * Integer multiplication with the semantics of Java {@code *}.
 */
public class Mul extends Primitive2 {
    @Override
    public ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type) {
        return ExpressionType.known(INT);
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        try {
            return (Integer) arg1 * (Integer) arg2;
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected();
        }
    }

    @Override
    protected JvmType generateForReferenceReference(GhostWriter writer) {
        writer.invokeStatic(Mul.class, "mul", int.class, Object.class, Object.class);
        return INT;
    }

    @Override
    protected JvmType generateForReferenceInt(GhostWriter writer) {
        writer.invokeStatic(Mul.class, "mul", int.class, Object.class, int.class);
        return INT;
    }

    @Override
    protected JvmType generateForReferenceBoolean(GhostWriter writer) {
        writer.throwError("cannot multiply by a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForIntReference(GhostWriter writer) {
        writer
            .adaptValue(REFERENCE, INT)
            .asm().visitInsn(IMUL);
        return INT;
    }

    @Override
    protected JvmType generateForIntInt(GhostWriter writer) {
        writer.asm().visitInsn(IMUL);
        return INT;
    }

    @Override
    protected JvmType generateForIntBoolean(GhostWriter writer) {
        writer.throwError("cannot multiply by a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForBooleanReference(GhostWriter writer) {
        writer.throwError("cannot multiply a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForBooleanInt(GhostWriter writer) {
        writer.throwError("cannot multiply a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForBooleanBoolean(GhostWriter writer) {
        writer.throwError("cannot multiply a boolean");
        return INT;
    }

    public static int mul(Object a, Object b) {
        return (Integer) a * (Integer) b;
    }

    public static int mul(Object a, int b) {
        return (Integer) a * b;
    }
    public static int mul(int a, Object b) {
        return a * (Integer) b;
    }
}
