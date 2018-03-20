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
import static org.objectweb.asm.Opcodes.IADD;

public class Add extends Primitive2 {
    public Add(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        super(argument1, argument2);
    }

    @Override
    public TypeCategory valueCategory() {
        return INT;
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 + (Integer) arg2;
    }

    @Override
    public TypeCategory generate(GhostWriter writer, TypeCategory arg1Category, TypeCategory arg2Category) {
        return arg1Category.match(new TypeCategory.Matcher<TypeCategory>() {
            public TypeCategory ifReference() {
                return arg2Category.match(new TypeCategory.Matcher<TypeCategory>() {
                    public TypeCategory ifReference() { // (Object, Object)
                        writer.invokeStatic(Add.class, "add", int.class, Object.class, Object.class);
                        return INT;
                    }
                    public TypeCategory ifInt() { // (Object, int)
                        writer.invokeStatic(Add.class, "add", int.class, Object.class, int.class);
                        return INT;
                    }
                    public TypeCategory ifBoolean() { // (Object, boolean)
                        throw new CompilerError("ADD is not applicable to a boolean");
                    }
                });
            }

            public TypeCategory ifInt() {
                return arg2Category.match(new TypeCategory.Matcher<TypeCategory>() {
                    public TypeCategory ifReference() { // (int, Object)
                        writer.invokeStatic(Add.class, "add", int.class, int.class, Object.class);
                        return INT;
                    }
                    public TypeCategory ifInt() { // (int, int)
                        writer.withAsmVisitor(it -> it.visitInsn(IADD));
                        return INT;
                    }
                    public TypeCategory ifBoolean() {
                        throw new CompilerError("ADD is not applicable to a boolean");
                    }
                });
            }

            public TypeCategory ifBoolean() {
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
}
