// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.AtomicExpression;
import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.Primitive2;
import com.github.vassilibykov.enfilade.core.TypeCategory;
import org.jetbrains.annotations.NotNull;

import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;
import static org.objectweb.asm.Opcodes.IMUL;

public class Mul extends Primitive2 {
    public Mul(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        super(argument1, argument2);
    }

    @Override
    public TypeCategory valueCategory() {
        return TypeCategory.INT;
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 * (Integer) arg2;
    }

    @Override
    public TypeCategory generate(GhostWriter writer, TypeCategory arg1Category, TypeCategory arg2Category) {
        return arg1Category.match(new TypeCategory.Matcher<TypeCategory>() {
            public TypeCategory ifReference() {
                return arg2Category.match(new TypeCategory.Matcher<TypeCategory>() {
                    public TypeCategory ifReference() { // (Object, Object)
                        writer.invokeStatic(Mul.class, "mul", int.class, Object.class, Object.class);
                        return INT;
                    }
                    public TypeCategory ifInt() { // (Object, int)
                        writer.invokeStatic(Mul.class, "mul", int.class, Object.class, int.class);
                        return INT;
                    }
                    public TypeCategory ifBoolean() { // (Object, boolean)
                        throw new CompilerError("MUL is not applicable to a boolean");
                    }
                });
            }

            public TypeCategory ifInt() {
                return arg2Category.match(new TypeCategory.Matcher<TypeCategory>() {
                    public TypeCategory ifReference() { // (int, Object)
                        writer
                            .adaptType(REFERENCE, INT)
                            .asm().visitInsn(IMUL);
                        return INT;
                    }
                    public TypeCategory ifInt() { // (int, int)
                        writer.asm().visitInsn(IMUL);
                        return INT;
                    }
                    public TypeCategory ifBoolean() {
                        throw new CompilerError("MUL is not applicable to a boolean");
                    }
                });
            }

            public TypeCategory ifBoolean() {
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
