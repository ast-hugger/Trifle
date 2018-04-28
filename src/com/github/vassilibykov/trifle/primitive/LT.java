// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.CompilerError;
import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.Primitive2Node;
import com.github.vassilibykov.trifle.core.PrimitiveNode;
import com.github.vassilibykov.trifle.core.RuntimeError;
import org.objectweb.asm.Opcodes;

import java.util.Optional;

import static com.github.vassilibykov.trifle.core.JvmType.BOOL;
import static com.github.vassilibykov.trifle.core.JvmType.INT;

/**
 * A less-than comparison of integers.
 */
public class LT extends Primitive2 implements IfAware {

    @Override
    public ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type) {
        return ExpressionType.known(BOOL);
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        try {
            return (Integer) arg1 < (Integer) arg2;
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected(arg1, arg2);
        }
    }

    @Override
    public JvmType generate(GhostWriter writer, JvmType arg1Category, JvmType arg2Category) {
        return arg1Category.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (Object, Object)
                        writer.invokeStatic(LT.class, "lessThan", boolean.class, Object.class, Object.class);
                        return BOOL;
                    }

                    public JvmType ifInt() { // (Object, int)
                        writer.invokeStatic(LT.class, "lessThan", boolean.class, Object.class, int.class);
                        return BOOL;
                    }

                    public JvmType ifBoolean() { // (Object, boolean)
                        throw new CompilerError("LT is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifInt() {
                return arg2Category.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (int, Object)
                        writer.invokeStatic(LT.class, "lessThan", boolean.class, int.class, Object.class);
                        return BOOL;
                    }

                    public JvmType ifInt() { // (int, int)
                        writer.invokeStatic(LT.class, "lessThan", boolean.class, int.class, int.class);
                        return BOOL;
                    }

                    public JvmType ifBoolean() {
                        throw new CompilerError("LT is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifBoolean() {
                throw new CompilerError("LT is not applicable to a boolean");
            }
        });
    }

    @Override
    public Optional<OptimizedIfForm> optimizedFormFor(PrimitiveNode ifCondition) {
        var primitive = (Primitive2Node) ifCondition; // cast must succeed
        if (primitive.argument1().specializedType() == INT
            && primitive.argument2().specializedType() == INT)
        {
            return Optional.of(new IfFormOptimizedForInts(primitive, Opcodes.IF_ICMPGE));
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean lessThan(Object a, Object b) {
        return (Integer) a < (Integer) b;
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean lessThan(Object a, int b) {
        return (Integer) a < b;
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean lessThan(int a, Object b) {
        return a < (Integer) b;
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean lessThan(int a, int b) {
        return a < b;
    }
}
