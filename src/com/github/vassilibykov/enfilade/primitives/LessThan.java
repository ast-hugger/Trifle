// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.AtomicExpression;
import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.Expression;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.If;
import com.github.vassilibykov.enfilade.core.Primitive2;
import com.github.vassilibykov.enfilade.core.TypeCategory;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.vassilibykov.enfilade.core.TypeCategory.BOOL;
import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;

public class LessThan extends Primitive2 {
    public LessThan(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        super(argument1, argument2);
    }

    @Override
    public TypeCategory valueCategory() {
        return TypeCategory.BOOL;
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 < (Integer) arg2;
    }

    @SuppressWarnings("unused") // called by generated code
    public static Object staticApply(Object arg1, Object arg2) {
        return (Integer) arg1 < (Integer) arg2;
    }

    public static boolean staticApply(int arg1, int arg2) {
        return arg1 < arg2;
    }

    @Override
    public TypeCategory generate(GhostWriter writer, TypeCategory arg1Category, TypeCategory arg2Category) {
        return arg1Category.match(new TypeCategory.Matcher<TypeCategory>() {
            public TypeCategory ifReference() {
                return arg2Category.match(new TypeCategory.Matcher<TypeCategory>() {
                    public TypeCategory ifReference() { // (Object, Object)
                        writer.invokeStatic(LessThan.class, "lessThan", boolean.class, Object.class, Object.class);
                        return BOOL;
                    }
                    public TypeCategory ifInt() { // (Object, int)
                        writer.invokeStatic(LessThan.class, "lessThan", boolean.class, Object.class, int.class);
                        return BOOL;
                    }
                    public TypeCategory ifBoolean() { // (Object, boolean)
                        throw new CompilerError("LT is not applicable to a boolean");
                    }
                });
            }

            public TypeCategory ifInt() {
                return arg2Category.match(new TypeCategory.Matcher<TypeCategory>() {
                    public TypeCategory ifReference() { // (int, Object)
                        writer.invokeStatic(LessThan.class, "lessThan", boolean.class, int.class, Object.class);
                        return BOOL;
                    }
                    public TypeCategory ifInt() { // (int, int)
                        writer.invokeStatic(LessThan.class, "lessThan", boolean.class, int.class, int.class);
                        return BOOL;
                    }
                    public TypeCategory ifBoolean() {
                        throw new CompilerError("SUB is not applicable to a boolean");
                    }
                });
            }

            public TypeCategory ifBoolean() {
                throw new CompilerError("SUB is not applicable to a boolean");
            }
        });
    }

    public static boolean lessThan(Object a, Object b) {
        return (Integer) a < (Integer) b;
    }

    public static boolean lessThan(Object a, int b) {
        return (Integer) a < b;
    }

    public static boolean lessThan(int a, Object b) {
        return a < (Integer) b;
    }

    public static boolean lessThan(int a, int b) {
        return a < b;
    }

    public void generateIf(
        BiConsumer<TypeCategory, Expression> argGenerator,
        Runnable trueBranchGenerator,
        Runnable falseBranchGenerator,
        GhostWriter writer)
    {
        argGenerator.accept(INT, argument1());
        argGenerator.accept(INT, argument2());
        writer.withLabelAtEnd(end -> {
            writer.withLabelAtEnd(elseStart -> {
                writer.asm().visitJumpInsn(IF_ICMPGE, elseStart);
                trueBranchGenerator.run();
                writer.jump(end);
            });
            falseBranchGenerator.run();
        });
    }
}
