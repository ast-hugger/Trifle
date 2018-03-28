// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.Primitive2Node;
import com.github.vassilibykov.enfilade.core.JvmType;
import org.jetbrains.annotations.NotNull;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static org.objectweb.asm.Opcodes.IADD;

public class Add extends Primitive2Node {
    public Add(@NotNull EvaluatorNode argument1, @NotNull EvaluatorNode argument2) {
        super(argument1, argument2);
    }

    @Override
    public JvmType jvmType() {
        return INT;
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 + (Integer) arg2;
    }

    @Override
    public JvmType generate(GhostWriter writer, JvmType arg1Category, JvmType arg2Category) {
        return arg1Category.match(new JvmType.Matcher<JvmType>() {
            public JvmType ifReference() {
                return arg2Category.match(new JvmType.Matcher<JvmType>() {
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
                return arg2Category.match(new JvmType.Matcher<JvmType>() {
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
        return (Integer) arg1 + (Integer) arg2;
    }

    @SuppressWarnings("unused") // called by generated code
    public static int add(Object arg1, int arg2) {
        return (Integer) arg1 + arg2;
    }

    @SuppressWarnings("unused") // called by generated code
    public static int add(int arg1, Object arg2) {
        return arg1 + (Integer) arg2;
    }

    @Override
    public String toString() {
        return "(ADD " + argument1() + " " + argument2() + ")";
    }
}
