// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.CompilerError;
import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.RuntimeError;

import static com.github.vassilibykov.trifle.core.JvmType.INT;
import static org.objectweb.asm.Opcodes.IADD;

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
    public JvmType generate(GhostWriter writer, JvmType arg1Category, JvmType arg2Category) {
        return arg1Category.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (Object, Object)
                        writer.invokeStatic(Add.class, "add", int.class, Object.class, Object.class);
                        return INT;
                    }

                    public JvmType ifInt() { // (Object, int)
                        writer.invokeStatic(Add.class, "add", int.class, Object.class, int.class);
                        return INT;
                    }

                    public JvmType ifBoolean() { // (Object, boolean)
                        throw new CompilerError("ADD is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifInt() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (int, Object)
                        writer.invokeStatic(Add.class, "add", int.class, int.class, Object.class);
                        return INT;
                    }

                    public JvmType ifInt() { // (int, int)
                        writer.asm().visitInsn(IADD);
                        return INT;
                    }

                    public JvmType ifBoolean() {
                        throw new CompilerError("ADD is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifBoolean() {
                throw new CompilerError("ADD is not applicable to a boolean");
            }
        });
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
