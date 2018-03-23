// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

/**
 * A convenience wrapper around ASM's {@link MethodVisitor} to more concisely
 * support our specific code writing needs.
 */
@SuppressWarnings("UnusedReturnValue") // normal in this class
public class GhostWriter {
    private static final int[] SPECIAL_LOAD_INT_OPCODES = new int[] {
        ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 };

    public static String internalClassName(Class<?> klass) {
        return internalClassName(klass.getName());
    }

    public static String internalClassName(String fqnName) {
        return fqnName.replace('.', '/');
    }

    /*
        Instance
     */

    private final MethodVisitor asmWriter;

    GhostWriter(MethodVisitor methodWriter) {
        this.asmWriter = methodWriter;
    }

    public MethodVisitor asm() {
        return asmWriter;
    }

    public GhostWriter adaptType(TypeCategory from, TypeCategory to) {
        from.match(new TypeCategory.VoidMatcher() {
            public void ifReference() {
                to.match(new TypeCategory.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { unboxInteger(); }
                    public void ifBoolean() { unboxBoolean(); }
                });
            }
            public void ifInt() {
                to.match(new TypeCategory.VoidMatcher() {
                    public void ifReference() { boxInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { throw new CompilerError("cannot adapt int to boolean"); }
                });
            }
            public void ifBoolean() {
                to.match(new TypeCategory.VoidMatcher() {
                    public void ifReference() { boxBoolean(); }
                    public void ifInt() {
                        throw new CompilerError("cannot adapt boolean to int");
                    }
                    public void ifBoolean() { }
                });
            }
        });
        return this;
    }

    public GhostWriter boxBoolean() {
        invokeStatic(Boolean.class, "valueOf", Boolean.class, boolean.class);
        return this;
    }

    public GhostWriter boxInteger() {
        invokeStatic(Integer.class, "valueOf", Integer.class, int.class);
        return this;
    }

    public GhostWriter checkCast(Class<?> castClass) {
        asmWriter.visitTypeInsn(CHECKCAST, internalClassName(castClass));
        return this;
    }

    public GhostWriter dup() {
        asmWriter.visitInsn(DUP);
        return this;
    }

    public GhostWriter invokeDynamic(Handle bootstrapper, String name, MethodType callSiteType, Object... bootstrapperArgs) {
        asmWriter.visitInvokeDynamicInsn(
            name,
            callSiteType.toMethodDescriptorString(),
            bootstrapper,
            bootstrapperArgs);
        return this;
    }

    public GhostWriter invokeStatic(Class<?> owner, String methodName, Class<?> returnType, Class<?>... argTypes) {
        asmWriter.visitMethodInsn(
            INVOKESTATIC,
            internalClassName(owner),
            methodName,
            MethodType.methodType(returnType, argTypes).toMethodDescriptorString(),
            false);
        return this;
    }

    public GhostWriter invokeVirtual(Class<?> owner, String methodName, Class<?> returnType, Class<?>... argTypes) {
        asmWriter.visitMethodInsn(
            INVOKEVIRTUAL,
            internalClassName(owner),
            methodName,
            MethodType.methodType(returnType, argTypes).toMethodDescriptorString(),
            false);
        return this;
    }

    public GhostWriter jump(Label label) {
        asmWriter.visitJumpInsn(GOTO, label);
        return this;
    }

    public GhostWriter jumpIf0(Label label) {
        asmWriter.visitJumpInsn(IFEQ, label);
        return this;
    }

    /**
     * Load an {@code int} constant on the stack using the best available
     * instruction.
     */
    public GhostWriter loadInt(int value) {
        if (0 <= value && value <= 5) {
            asmWriter.visitInsn(SPECIAL_LOAD_INT_OPCODES[value]);
        } else if (-128 <= value && value <= 127) {
            asmWriter.visitIntInsn(BIPUSH, value);
        } else {
            asmWriter.visitIntInsn(SIPUSH, value);
        }
        return this;
    }

    public GhostWriter loadLocal(TypeCategory category, int index) {
        switch (category) {
            case REFERENCE:
                asmWriter.visitVarInsn(ALOAD, index);
                break;
            case INT:
                asmWriter.visitVarInsn(ILOAD, index);
                break;
            default:
                throw new IllegalArgumentException("unrecognized type category");
        }
        return this;
    }

    public GhostWriter loadNull() {
        asmWriter.visitInsn(ACONST_NULL);
        return this;
    }

    public GhostWriter loadString(String string) {
        asmWriter.visitLdcInsn(string);
        return this;
    }

    public GhostWriter pop() {
        asmWriter.visitInsn(POP);
        return this;
    }

    public GhostWriter ret(TypeCategory category) {
        switch (category) {
            case REFERENCE:
                asmWriter.visitInsn(ARETURN);
                break;
            case INT:
                asmWriter.visitInsn(IRETURN);
                break;
            default:
                throw new IllegalArgumentException("unrecognized type category");
        }
        return this;
    }

    public GhostWriter storeLocal(TypeCategory category, int index) {
        switch (category) {
            case REFERENCE:
                asmWriter.visitVarInsn(ASTORE, index);
                break;
            case INT:
                asmWriter.visitVarInsn(ISTORE, index);
                break;
            default:
                throw new IllegalArgumentException("unrecognized type category");
        }
        return this;
    }

    public GhostWriter swap() {
        asmWriter.visitInsn(SWAP);
        return this;
    }

    public GhostWriter throwSquarePegException() {
        invokeStatic(SquarePegException.class, "with", SquarePegException.class, Object.class);
        asmWriter.visitInsn(ATHROW);
        return this;
    }

    public GhostWriter unboxBoolean() {
        checkCast(Boolean.class);
        invokeVirtual(Boolean.class, "booleanValue", boolean.class);
        return this;
    }

    public GhostWriter unboxInteger() {
        checkCast(Integer.class);
        invokeVirtual(Integer.class, "intValue", int.class);
        return this;
    }

    public GhostWriter withLabelAtEnd(Consumer<Label> emitter) {
        Label label = new Label();
        emitter.accept(label);
        asmWriter.visitLabel(label);
        return this;
    }
}
