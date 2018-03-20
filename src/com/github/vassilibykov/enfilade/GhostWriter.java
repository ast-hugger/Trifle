package com.github.vassilibykov.enfilade;

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

    private final MethodVisitor methodWriter;

    GhostWriter(MethodVisitor methodWriter) {
        this.methodWriter = methodWriter;
    }

    public MethodVisitor methodWriter() {
        return methodWriter;
    }

    public GhostWriter checkCast(Class<?> castClass) {
        methodWriter.visitTypeInsn(CHECKCAST, internalClassName(castClass));
        return this;
    }

    public GhostWriter dup() {
        methodWriter.visitInsn(DUP);
        return this;
    }

    public GhostWriter invokeDynamic(Handle bootstrapper, String name, MethodType callSiteType, Object... bootstrapperArgs) {
        methodWriter.visitInvokeDynamicInsn(
            name,
            callSiteType.toMethodDescriptorString(),
            bootstrapper,
            bootstrapperArgs);
        return this;
    }

    public GhostWriter invokeStatic(Class<?> owner, String methodName, Class<?> returnType, Class<?>... argTypes) {
        methodWriter.visitMethodInsn(
            INVOKESTATIC,
            internalClassName(owner),
            methodName,
            MethodType.methodType(returnType, argTypes).toMethodDescriptorString(),
            false);
        return this;
    }

    public GhostWriter invokeVirtual(Class<?> owner, String methodName, Class<?> returnType, Class<?>... argTypes) {
        methodWriter.visitMethodInsn(
            INVOKEVIRTUAL,
            internalClassName(owner),
            methodName,
            MethodType.methodType(returnType, argTypes).toMethodDescriptorString(),
            false);
        return this;
    }

    public GhostWriter jump(Label label) {
        methodWriter.visitJumpInsn(GOTO, label);
        return this;
    }

    public GhostWriter jumpIf0(Label label) {
        methodWriter.visitJumpInsn(IFEQ, label);
        return this;
    }

    /**
     * Load an {@code int} constant on the stack using the best available
     * instruction.
     */
    public GhostWriter loadInt(int value) {
        if (0 <= value && value <= 5) {
            methodWriter.visitInsn(SPECIAL_LOAD_INT_OPCODES[value]);
        } else if (-128 <= value && value <= 127) {
            methodWriter.visitIntInsn(BIPUSH, value);
        } else {
            methodWriter.visitIntInsn(SIPUSH, value);
        }
        return this;
    }

    public GhostWriter loadLocal(TypeCategory category, int index) {
        switch (category) {
            case REFERENCE:
                methodWriter.visitVarInsn(ALOAD, index);
                break;
            case INT:
                methodWriter.visitVarInsn(ILOAD, index);
                break;
            default:
                throw new IllegalArgumentException("unrecognized type category");
        }
        return this;
    }

    public GhostWriter loadNull() {
        methodWriter.visitInsn(ACONST_NULL);
        return this;
    }

    public GhostWriter loadString(String string) {
        methodWriter.visitLdcInsn(string);
        return this;
    }

    public GhostWriter pop() {
        methodWriter.visitInsn(POP);
        return this;
    }

    public GhostWriter ret(TypeCategory category) {
        switch (category) {
            case REFERENCE:
                methodWriter.visitInsn(ARETURN);
                break;
            case INT:
                methodWriter.visitInsn(IRETURN);
                break;
            default:
                throw new IllegalArgumentException("unrecognized type category");
        }
        return this;
    }

    public GhostWriter storeLocal(TypeCategory category, int index) {
        switch (category) {
            case REFERENCE:
                methodWriter.visitVarInsn(ASTORE, index);
                break;
            case INT:
                methodWriter.visitVarInsn(ISTORE, index);
                break;
            default:
                throw new IllegalArgumentException("unrecognized type category");
        }
        return this;
    }

    public GhostWriter swap() {
        methodWriter.visitInsn(SWAP);
        return this;
    }

    public GhostWriter withLabelAtTheEnd(Consumer<Label> emitter) {
        Label label = new Label();
        emitter.accept(label);
        methodWriter.visitLabel(label);
        return this;
    }
}
