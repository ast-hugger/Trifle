// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.RuntimeError;

import static com.github.vassilibykov.trifle.core.JvmType.INT;
import static org.objectweb.asm.Opcodes.IADD;

/**
 * Integer addition with the semantics of Java {@code +}
 * (overflow by wrapping around).
 */
public class Add extends Primitive2 {

    @Override
    public ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type) {
        return ExpressionType.known(INT);
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        try {
            return (Integer) arg1 + (Integer) arg2;
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected();
        }
    }

    @Override
    protected JvmType generateForReferenceReference(GhostWriter writer) {
        writer.invokeStatic(Add.class, "add", int.class, Object.class, Object.class);
        return INT;
    }

    @Override
    protected JvmType generateForReferenceInt(GhostWriter writer) {
        writer.invokeStatic(Add.class, "add", int.class, Object.class, int.class);
        return INT;
    }

    @Override
    protected JvmType generateForReferenceBoolean(GhostWriter writer) {
        writer.throwError("cannot add a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForIntReference(GhostWriter writer) {
        writer.invokeStatic(Add.class, "add", int.class, int.class, Object.class);
        return INT;
    }

    @Override
    protected JvmType generateForIntInt(GhostWriter writer) {
        writer.asm().visitInsn(IADD);
        return INT;
    }

    @Override
    protected JvmType generateForIntBoolean(GhostWriter writer) {
        writer.throwError("cannot add a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForBooleanReference(GhostWriter writer) {
        writer.throwError("cannot add a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForBooleanInt(GhostWriter writer) {
        writer.throwError("cannot add a boolean");
        return INT;
    }

    @Override
    protected JvmType generateForBooleanBoolean(GhostWriter writer) {
        writer.throwError("cannot add a boolean");
        return INT;
    }

    @SuppressWarnings("unused") // called by generated code
    public static int add(Object arg1, Object arg2) {
        try {
            return (Integer) arg1 + (Integer) arg2;
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected();
        }
    }

    @SuppressWarnings("unused") // called by generated code
    public static int add(Object arg1, int arg2) {
        try {
            return (Integer) arg1 + arg2;
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected();
        }
    }

    @SuppressWarnings("unused") // called by generated code
    public static int add(int arg1, Object arg2) {
        try {
            return arg1 + (Integer) arg2;
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected();
        }
    }
}
