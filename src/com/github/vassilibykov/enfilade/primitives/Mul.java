// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.Primitive2Node;
import com.github.vassilibykov.enfilade.core.JvmType;
import org.jetbrains.annotations.NotNull;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static org.objectweb.asm.Opcodes.IMUL;

public class Mul extends Primitive2Node {
    public Mul(@NotNull EvaluatorNode argument1, @NotNull EvaluatorNode argument2) {
        super(argument1, argument2);
    }

    @Override
    public JvmType valueCategory() {
        return JvmType.INT;
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 * (Integer) arg2;
    }

    @Override
    public JvmType generate(GhostWriter writer, JvmType arg1Category, JvmType arg2Category) {
        return arg1Category.match(new JvmType.Matcher<JvmType>() {
            public JvmType ifReference() {
                return arg2Category.match(new JvmType.Matcher<JvmType>() {
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
                return arg2Category.match(new JvmType.Matcher<JvmType>() {
                    public JvmType ifReference() { // (int, Object)
                        writer
                            .adaptType(REFERENCE, INT)
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

    @Override
    public String toString() {
        return "(MUL " + argument1() + " " + argument2() + ")";
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
