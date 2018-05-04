// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.Primitive2Node;
import com.github.vassilibykov.trifle.core.PrimitiveNode;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.Objects;
import java.util.Optional;

import static com.github.vassilibykov.trifle.core.JvmType.BOOL;
import static com.github.vassilibykov.trifle.core.JvmType.INT;
import static com.github.vassilibykov.trifle.core.JvmType.REFERENCE;

/**
 * Object equivalence, generally in the sense of {@code ==}. Exceptions are
 * comparisons of Integer and Boolean wrapper instances, which should be
 * indistinguishable from comparing their underlying primitive values.
 */
public class EQ extends Primitive2 implements IfAware {

    @Override
    public ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type) {
        return ExpressionType.known(BOOL);
    }

    @Override
    public Object apply(Object argument1, Object argument2) {
        if (argument1 instanceof Integer || argument1 instanceof Boolean) {
            return Objects.equals(argument1, argument2);
        } else {
            return argument1 == argument2;
        }
    }

    @Override
    protected JvmType generateForReferenceReference(GhostWriter writer) {
        writer.invokeStatic(Objects.class, "equals", boolean.class, Object.class, Object.class);
        return BOOL;
    }

    @Override
    protected JvmType generateForReferenceInt(GhostWriter writer) {
        writer.swap();
        writer.ensureValue(REFERENCE, INT);
        generateCompareInts(writer);
        return BOOL;
    }

    @Override
    protected JvmType generateForReferenceBoolean(GhostWriter writer) {
        writer.swap();
        writer.ensureValue(REFERENCE, BOOL);
        generateCompareInts(writer);
        return BOOL;
    }

    @Override
    protected JvmType generateForIntReference(GhostWriter writer) {
        writer.ensureValue(REFERENCE, INT);
        generateCompareInts(writer);
        return BOOL;
    }

    @Override
    protected JvmType generateForIntInt(GhostWriter writer) {
        generateCompareInts(writer);
        return BOOL;
    }

    @Override
    protected JvmType generateForIntBoolean(GhostWriter writer) {
        writer.loadInt(0);
        return BOOL;
    }

    @Override
    protected JvmType generateForBooleanReference(GhostWriter writer) {
        writer.ensureValue(REFERENCE, BOOL);
        generateCompareInts(writer);
        return BOOL;
    }

    @Override
    protected JvmType generateForBooleanInt(GhostWriter writer) {
        writer.loadInt(0);
        return BOOL;
    }

    @Override
    protected JvmType generateForBooleanBoolean(GhostWriter writer) {
        generateCompareInts(writer);
        return BOOL;
    }

    private void generateCompareInts(GhostWriter writer) {
        writer.withLabelAtEnd(end -> {
            var notEqual = new Label();
            writer.asm().visitJumpInsn(Opcodes.IF_ICMPNE, notEqual);
            writer
                .loadInt(1)
                .jump(end);
            writer.setLabelHere(notEqual);
            writer.loadInt(0);
        });
    }

    @Override
    public Optional<OptimizedIfForm> optimizedFormFor(PrimitiveNode ifCondition) {
        var primitive = (Primitive2Node) ifCondition;
        var arg1type = primitive.argument1().specializedType();
        var arg2type = primitive.argument2().specializedType();
        if ((arg1type == INT && arg2type == INT) || (arg1type == BOOL && arg2type == BOOL)) {
            return Optional.of(new IfFormOptimizedForInts(primitive, Opcodes.IF_ICMPNE));
        } else {
            return Optional.empty();
        }
    }
}
