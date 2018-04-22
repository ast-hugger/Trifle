// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitive;

import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.ExpressionType;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.JvmType;
import com.github.vassilibykov.enfilade.core.RuntimeError;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static org.objectweb.asm.Opcodes.IMUL;

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
    public JvmType generate(GhostWriter writer, JvmType arg1Category, JvmType arg2Category) {
        return arg1Category.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (Object, Object)
                        writer.invokeStatic(Mul.class, "mul", int.class, Object.class, Object.class);
                        return INT;
                    }

                    public JvmType ifInt() { // (Object, int)
                        writer.invokeStatic(Mul.class, "mul", int.class, Object.class, int.class);
                        return INT;
                    }

                    public JvmType ifBoolean() { // (Object, boolean)
                        throw new CompilerError("MUL is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifInt() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (int, Object)
                        writer
                            .adaptValue(REFERENCE, INT)
                            .asm().visitInsn(IMUL);
                        return INT;
                    }

                    public JvmType ifInt() { // (int, int)
                        writer.asm().visitInsn(IMUL);
                        return INT;
                    }

                    public JvmType ifBoolean() {
                        throw new CompilerError("MUL is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifBoolean() {
                throw new CompilerError("MUL is not applicable to a boolean");
            }
        });
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
