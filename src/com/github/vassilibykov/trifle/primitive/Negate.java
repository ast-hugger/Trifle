// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.RuntimeError;

import static com.github.vassilibykov.trifle.core.JvmType.INT;
import static com.github.vassilibykov.trifle.core.JvmType.REFERENCE;
import static org.objectweb.asm.Opcodes.ISUB;

public class Negate extends Primitive1 {
    @Override
    public ExpressionType inferredType(ExpressionType argumentType) {
        return ExpressionType.known(INT);
    }

    @Override
    public Object apply(Object arg) {
        try {
            return -((Integer) arg);
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected(arg);
        }
    }

    @Override
    protected JvmType generateForReference(GhostWriter writer) {
        writer
            .adaptValue(REFERENCE, INT)
            .loadInt(0)
            .swap()
            .asm().visitInsn(ISUB);
        return INT;
    }

    @Override
    protected JvmType generateForInt(GhostWriter writer) {
        writer
            .loadInt(0)
            .swap()
            .asm().visitInsn(ISUB);
        return INT;
    }

    @Override
    protected JvmType generateForBoolean(GhostWriter writer) {
        writer.throwError("cannot negate a boolean");
        return INT;
    }
}
