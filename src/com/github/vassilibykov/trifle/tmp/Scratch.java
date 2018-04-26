// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.tmp;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import sun.misc.Unsafe;

import static org.objectweb.asm.Opcodes.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.function.Consumer;

public class Scratch {
    private static final Unsafe UNSAFE = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            return (Unsafe) singleoneInstanceField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void main(String[] args) {
        var bytecode = withClassWriter(classWriter -> {
            withMethodWriter(classWriter, methodWriter -> {
                methodWriter.visitInsn(ICONST_0);
                methodWriter.visitVarInsn(ISTORE, 1);
                methodWriter.visitLdcInsn("hello");
                methodWriter.visitVarInsn(ASTORE, 1);
                methodWriter.visitVarInsn(ALOAD, 1);
                methodWriter.visitInsn(ARETURN);
            });
        });
        Class<?> generatedClass = UNSAFE.defineAnonymousClass(Scratch.class, bytecode, null);
        try {
            MethodHandle testMethod = MethodHandles.lookup().findStatic(
                generatedClass, "test", MethodType.methodType(Object.class));
            var result = (Object) testMethod.invoke();
            System.out.println(result);
        } catch (Throwable  e) {
            throw new AssertionError(e);
        }
    }

    private static byte[] withClassWriter(Consumer<ClassWriter> writer) {
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(
            V9,
            ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
            "foobar",
            null,
            "java/lang/Object",
            null);
        writer.accept(classWriter);
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private static void withMethodWriter(ClassWriter classWriter, Consumer<MethodVisitor> methodGenerator) {
        var methodVisitor = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "test",
            MethodType.methodType(Object.class).toMethodDescriptorString(),
            null, null);
        methodGenerator.accept(methodVisitor);
        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }
}
