// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodType;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

/**
 * Compiles a {@link Function} into a class with a single static method.
 */
public class Compiler {

    public static final String GENERIC_METHOD_NAME = "generic";
    public static final String SPECIALIZED_METHOD_NAME = "specialized";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final String GENERATED_CLASS_NAME_PREFIX = "$g$";

    /**
     * The access point: compile a function.
     */
    public static Result compile(Function function) {
        Compiler compiler = new Compiler(function);
        return compiler.compile();
    }

    public static class Result {
        private final String className;
        private final byte[] bytecode;
        @Nullable private final MethodType specializationType; // set by generateSpecializedMethod()

        private Result(String className, byte[] bytecode, @Nullable MethodType specializationType) {
            this.className = className;
            this.bytecode = bytecode;
            this.specializationType = specializationType;
        }

        public String className() {
            return className;
        }

        public byte[] bytecode() {
            return bytecode;
        }

        @Nullable
        public MethodType specializationType() {
            return specializationType;
        }
    }

    static String internalClassName(Class<?> klass) {
        return internalClassName(klass.getName());
    }

    static String internalClassName(String fqnName) {
        return fqnName.replace('.', '/');
    }

    static String allocateClassName() {
        return GENERATED_CLASS_NAME_PREFIX + serial++;
    }

    private static long serial = 0;

    /*
        Instance
     */

    private final Function function;
    private final String className;
    private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    private TypeCategory bodyType;
    @Nullable private MethodType specializationType = null;

    private Compiler(Function function) {
        this.function = function;
        this.className = allocateClassName();
    }

    public Result compile() {
        bodyType = ExpressionTypeAnalyzer.analyze(function);
        setupClassWriter();
        generateGenericMethod();
        if (function.profile.canBeSpecialized()) {
            generateSpecializedMethod();
        }
        classWriter.visitEnd();
        return new Result(className, classWriter.toByteArray(), specializationType);
    }

    private void setupClassWriter() {
        classWriter.visit(
            Opcodes.V9,
            ACC_PUBLIC | ACC_SUPER | ACC_FINAL,
            internalClassName(className),
            null,
            JAVA_LANG_OBJECT,
            null);
    }

    private void generateGenericMethod() {
        MethodVisitor methodWriter = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            GENERIC_METHOD_NAME,
            MethodType.genericMethodType(function.arity()).toMethodDescriptorString(),
            null, null);
        methodWriter.visitCode();
        FunctionCodeGeneratorGeneric generator = new FunctionCodeGeneratorGeneric(methodWriter);
        TypeCategory resultType = function.body().accept(generator);
        generator.writer.adaptType(resultType, TypeCategory.REFERENCE);
        methodWriter.visitInsn(Opcodes.ARETURN);
        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
    }

    private void generateSpecializedMethod() {
        specializationType = computeSpecializationType();
        System.out.println("generating a specialized method of type " + specializationType);
        MethodVisitor methodWriter = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            SPECIALIZED_METHOD_NAME,
            specializationType.toMethodDescriptorString(),
            null, null);
        methodWriter.visitCode();
        FunctionCodeGeneratorSpecialized generator = new FunctionCodeGeneratorSpecialized(methodWriter);
        function.body().accept(generator);
        bodyType.match(new TypeCategory.VoidMatcher() {
            public void ifReference() { methodWriter.visitInsn(Opcodes.ARETURN); }
            public void ifInt() { methodWriter.visitInsn(Opcodes.IRETURN); }
            public void ifBoolean() { methodWriter.visitInsn(Opcodes.IRETURN); }
        });
        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
    }

    private MethodType computeSpecializationType() {
        Class<?>[] argClasses = Stream.of(function.arguments())
            .map(var -> var.compilerAnnotation.valueCategory().representativeType())
            .toArray(Class[]::new);
        return MethodType.methodType(bodyType.representativeType(), argClasses);
    }
}
