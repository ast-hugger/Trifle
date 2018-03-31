// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
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

    private static final String OBJECT_DESC = "Ljava/lang/Object;";
    private static final String BOOL_DESC = "Z";
    private static final String INT_DESC = "I";
    private static final String BOXED_REFERENCE_ICN = internalClassName(BoxedReference.class);
    private static final String BOXED_BOOL_ICN = internalClassName(BoxedBool.class);
    private static final String BOXED_INT_ICN = internalClassName(BoxedInt.class);

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

    public GhostWriter adaptType(JvmType from, JvmType to) {
        from.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { unboxInteger(); }
                    public void ifBoolean() { unboxBoolean(); }
                });
            }
            public void ifInt() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { boxInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { throw new CompilerError("cannot adapt int to boolean"); }
                });
            }
            public void ifBoolean() {
                to.match(new JvmType.VoidMatcher() {
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

    public GhostWriter loadDefaultValue(JvmType type) {
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() { loadNull(); }
            public void ifInt() { loadInt(0); }
            public void ifBoolean() { loadInt(0); }
        });
        return this;
    }

    public GhostWriter dup() {
        asmWriter.visitInsn(DUP);
        return this;
    }

    public GhostWriter extractBoxedVariable() {
        asmWriter.visitTypeInsn(CHECKCAST, BOXED_REFERENCE_ICN);
        asmWriter.visitFieldInsn(GETFIELD, BOXED_REFERENCE_ICN, "value", OBJECT_DESC);
        return this;
    }

    public GhostWriter handleSquarePegException(Label begin, Label end, Label handler) {
        asmWriter.visitTryCatchBlock(begin, end, handler, SquarePegException.INTERNAL_CLASS_NAME);
        return this;
    }

    public GhostWriter ifThenElse(Runnable trueBranchGenerator, Runnable falseBranchGenerator) {
        withLabelAtEnd(end -> {
            withLabelAtEnd(elseStart -> {
                jumpIf0(elseStart);
                trueBranchGenerator.run();
                jump(end);
            });
            falseBranchGenerator.run();
        });
        return this;
    }

    /**
     * Generate code to set the local slot at the specified index to contain a box of the
     * requested type. The value in the box is the value on the stack.
     */
    public GhostWriter initBoxedVariable(JvmType type, int index) {
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() { initBoxedReference(index); }
            public void ifBoolean() { initBoxedBool(index); }
            public void ifInt() { initBoxedInt(index); }
        });
        return this;
    }

    public GhostWriter initBoxedReference(int index) {
        invokeStatic(BoxedReference.class, "with", BoxedReference.class, Object.class);
        asmWriter.visitVarInsn(ASTORE, index);
        return this;
    }

    public GhostWriter initBoxedBool(int index) {
        invokeStatic(BoxedBool.class, "with", BoxedBool.class, boolean.class);
        asmWriter.visitVarInsn(ASTORE, index);
        return this;
    }

    public GhostWriter initBoxedInt(int index) {
        invokeStatic(BoxedInt.class, "with", BoxedInt.class, int.class);
        asmWriter.visitVarInsn(ASTORE, index);
        return this;
    }

    public GhostWriter instanceOf(Class<?> targetClass) {
        asmWriter.visitTypeInsn(INSTANCEOF, internalClassName(targetClass));
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

    public GhostWriter loadLocal(JvmType type, int index) {
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() { asmWriter.visitVarInsn(ALOAD, index); }
            public void ifInt() { asmWriter.visitVarInsn(ILOAD, index); }
            public void ifBoolean() { asmWriter.visitVarInsn(ILOAD, index); }
        });
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

    public GhostWriter newObjectArray(int size) {
        loadInt(size);
        asmWriter.visitTypeInsn(ANEWARRAY, internalClassName(Object.class));
        return this;
    }

    public GhostWriter pop() {
        asmWriter.visitInsn(POP);
        return this;
    }

    public GhostWriter ret(JvmType category) {
        // FIXME: 3/23/18 change to pattern matching style
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

    public GhostWriter storeArray(int index, Runnable valueGenerator) {
        dup();
        loadInt(index);
        valueGenerator.run();
        asmWriter.visitInsn(AASTORE);
        return this;
    }

    public GhostWriter storeLocal(JvmType type, int index) {
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() { asmWriter.visitVarInsn(ASTORE, index); }
            public void ifInt() { asmWriter.visitVarInsn(ISTORE, index); }
            public void ifBoolean() { asmWriter.visitVarInsn(ISTORE, index); }
        });
        return this;
    }

    public GhostWriter storeBoxedVariable(JvmType type, int index) {
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() { storeBoxedReference(index); }
            public void ifBoolean() { storeBoxedBool(index); }
            public void ifInt() { storeBoxedInt(index); }
        });
        return this;
    }

    public GhostWriter storeBoxedReference(int index) {
        asmWriter.visitVarInsn(ALOAD, index);
        asmWriter.visitTypeInsn(CHECKCAST, BOXED_REFERENCE_ICN);
        swap();
        asmWriter.visitFieldInsn(PUTFIELD, BOXED_REFERENCE_ICN, "value", OBJECT_DESC);
        return this;
    }

    public GhostWriter storeBoxedBool(int index) {
        asmWriter.visitVarInsn(ALOAD, index);
        asmWriter.visitTypeInsn(CHECKCAST, BOXED_BOOL_ICN);
        swap();
        asmWriter.visitFieldInsn(PUTFIELD, BOXED_BOOL_ICN, "value", BOOL_DESC);
        return this;
    }

    public GhostWriter storeBoxedInt(int index) {
        asmWriter.visitVarInsn(ALOAD, index);
        asmWriter.visitTypeInsn(CHECKCAST, BOXED_INT_ICN);
        swap();
        asmWriter.visitFieldInsn(PUTFIELD, BOXED_INT_ICN, "value", INT_DESC);
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

    /**
     * Generate code to unbox the value currently on the stack, which must be
     * a box of the appropriate type.
     */
    public GhostWriter unboxValue(JvmType type) {
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                checkCast(BoxedReference.class);
                asmWriter.visitFieldInsn(GETFIELD, BOXED_REFERENCE_ICN, "value", OBJECT_DESC);
            }
            public void ifInt() {
                checkCast(BoxedInt.class);
                asmWriter.visitFieldInsn(GETFIELD, BOXED_INT_ICN, "value", INT_DESC);
            }
            public void ifBoolean() {
                checkCast(BoxedBool.class);
                asmWriter.visitFieldInsn(GETFIELD, BOXED_BOOL_ICN, "value", BOOL_DESC);
            }
        });
        return this;
    }

    public GhostWriter withLabelAtEnd(Consumer<Label> emitter) {
        Label label = new Label();
        emitter.accept(label);
        asmWriter.visitLabel(label);
        return this;
    }

    public GhostWriter withLabelsAround(BiConsumer<Label, Label> emitter) {
        Label start = new Label();
        Label end = new Label();
        asmWriter.visitLabel(start);
        emitter.accept(start, end);
        asmWriter.visitLabel(end);
        return this;
    }
}
