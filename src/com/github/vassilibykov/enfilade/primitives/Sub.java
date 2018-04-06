// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.ExpressionType;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.JvmType;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static org.objectweb.asm.Opcodes.ISUB;

public class Sub extends Primitive2 {
    @Override
    public ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type) {
        return ExpressionType.known(INT);
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 - (Integer) arg2;
    }

    @Override
    public JvmType generate(GhostWriter writer, JvmType arg1Category, JvmType arg2Category) {
        return arg1Category.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (Object, Object)
                        writer.invokeStatic(Sub.class, "sub", int.class, Object.class, Object.class);
                        return INT;
                    }

                    public JvmType ifInt() { // (Object, int)
                        writer.invokeStatic(Sub.class, "sub", int.class, Object.class, int.class);
                        return INT;
                    }

                    public JvmType ifBoolean() { // (Object, boolean)
                        throw new CompilerError("SUB is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifInt() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (int, Object)
                        writer.invokeStatic(Sub.class, "sub", int.class, int.class, Object.class);
                        return INT;
                    }

                    public JvmType ifInt() { // (int, int)
                        writer.asm().visitInsn(ISUB);
                        return INT;
                    }

                    public JvmType ifBoolean() {
                        throw new CompilerError("SUB is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifBoolean() {
                throw new CompilerError("SUB is not applicable to a boolean");
            }
        });
    }

    @SuppressWarnings("unused") // called by generated code
    public static int sub(Object arg1, Object arg2) {
        return (Integer) arg1 - (Integer) arg2;
    }

    @SuppressWarnings("unused") // called by generated code
    public static int sub(Object arg1, int arg2) {
        return (Integer) arg1 - arg2;
    }

    @SuppressWarnings("unused") // called by generated code
    public static int sub(int arg1, Object arg2) {
        return arg1 - (Integer) arg2;
    }

}
