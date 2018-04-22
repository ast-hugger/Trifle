// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitive;

import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.ExpressionType;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.JvmType;
import com.github.vassilibykov.enfilade.core.RuntimeError;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
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
    public JvmType generate(GhostWriter writer, JvmType argCategory) {
        return argCategory.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                writer
                    .adaptValue(REFERENCE, INT)
                    .loadInt(0)
                    .swap()
                    .asm().visitInsn(ISUB);
                return INT;
            }

            public JvmType ifInt() {
                writer
                    .loadInt(0)
                    .swap()
                    .asm().visitInsn(ISUB);
                return INT;
            }

            public JvmType ifBoolean() {
                throw new CompilerError("NEGATE is not applicable to a boolean");
            }
        });
    }
}
