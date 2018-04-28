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
 * A greater-than comparison of integers.
 */
public class GT extends Primitive2 implements IfAware {
    @Override
    public ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type) {
        return ExpressionType.known(BOOL);
    }

    @Override
    public Object apply(Object argument1, Object argument2) {
        try {
            return (Integer) argument1 > (Integer) argument2;
        } catch (ClassCastException e) {
            throw RuntimeError.integerExpected(argument1, argument2);
        }
    }

    @Override
    public JvmType generate(GhostWriter writer, JvmType arg1Type, JvmType arg2Type) {
        return arg1Type.match(new JvmType.Matcher<>() {
            public JvmType ifReference() {
                return arg2Type.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (Object, Object)
                        writer.invokeStatic(GT.class, "greaterThan", boolean.class, Object.class, Object.class);
                        return BOOL;
                    }

                    public JvmType ifInt() { // (Object, int)
                        writer.invokeStatic(GT.class, "greaterThan", boolean.class, Object.class, int.class);
                        return BOOL;
                    }

                    public JvmType ifBoolean() { // (Object, boolean)
                        throw new CompilerError("GT is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifInt() {
                return arg2Type.match(new JvmType.Matcher<>() {
                    public JvmType ifReference() { // (int, Object)
                        writer.invokeStatic(GT.class, "greaterThan", boolean.class, int.class, Object.class);
                        return BOOL;
                    }

                    public JvmType ifInt() { // (int, int)
                        writer.invokeStatic(GT.class, "greaterThan", boolean.class, int.class, int.class);
                        return BOOL;
                    }

                    public JvmType ifBoolean() {
                        throw new CompilerError("GT is not applicable to a boolean");
                    }
                });
            }

            public JvmType ifBoolean() {
                throw new CompilerError("GT is not applicable to a boolean");
            }
        });
    }

    @Override
    public Optional<OptimizedIfForm> optimizedFormFor(PrimitiveNode ifCondition) {
        var primitive = (Primitive2Node) ifCondition; // cast must succeed
        if (primitive.argument1().specializedType() == INT
            && primitive.argument2().specializedType() == INT)
        {
            return Optional.of(new IfFormOptimizedForInts(primitive, Opcodes.IF_ICMPLE));
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean greaterThan(Object a, Object b) {
        return (Integer) a > (Integer) b;
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean greaterThan(Object a, int b) {
        return (Integer) a > b;
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean greaterThan(int a, Object b) {
        return a > (Integer) b;
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean greaterThan(int a, int b) {
        return a > b;
    }
}
