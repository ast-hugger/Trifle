// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

/**
 * Compiles a {@link FunctionImplementation} into a class with a single static method.
 */
public class Compiler {

    public static final String GENERIC_METHOD_NAME = "generic";
    public static final String SPECIALIZED_METHOD_NAME = "specialized";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final String GENERATED_CODE_PACKAGE = "com.github.vassilibykov.enfilade.core";
    private static final String GENERATED_CLASS_NAME_PREFIX = GENERATED_CODE_PACKAGE + ".$gen$";

    /**
     * The access point: compile a function.
     */
    public static BatchResult compile(FunctionImplementation topLevelFunction) {
        Compiler compiler = new Compiler(topLevelFunction);
        BatchResult batchResult = compiler.compile();
        dumpClassFile("generated", batchResult.bytecode());
        return batchResult;
    }

    private static void dumpClassFile(String name, byte[] bytecode) {
        File classFile = new File(name + ".class");
        try {
            FileOutputStream classStream = new FileOutputStream(classFile);
            classStream.write(bytecode);
            classStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class BatchResult {
        private final String className;
        private final byte[] bytecode;
        private final Map<FunctionImplementation, FunctionCompilationResult> results;

        private BatchResult(String className,
                            byte[] bytecode,
                            Map<FunctionImplementation, FunctionCompilationResult> results)
        {
            this.className = className;
            this.bytecode = bytecode;
            this.results = results;
        }

        String className() {
            return className;
        }

        byte[] bytecode() {
            return bytecode;
        }

        Map<FunctionImplementation, FunctionCompilationResult> results() {
            return results;
        }
    }

    static class FunctionCompilationResult {
        @NotNull private final String genericMethodName;
        @Nullable private final String specializedMethodName;
        @Nullable private final MethodType specializedMethodType;

        FunctionCompilationResult(@NotNull String genericMethodName,
                                  @Nullable String specializedMethodName,
                                  @Nullable MethodType specializedMethodType)
        {
            this.genericMethodName = genericMethodName;
            this.specializedMethodName = specializedMethodName;
            this.specializedMethodType = specializedMethodType;
        }

        String genericMethodName() {
            return genericMethodName;
        }

        String specializedMethodName() {
            return specializedMethodName;
        }

        MethodType specializedMethodType() {
            return specializedMethodType;
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

    private final FunctionImplementation topLevelFunction;
    private final String className;
    private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    @Nullable private String specializedMethodName = null;
    @Nullable private MethodType specializationType = null;
    private int generatedMethodSerial = 0;
    private final Map<FunctionImplementation, FunctionCompilationResult> individualResults = new HashMap<>();

    private Compiler(FunctionImplementation topLevelFunction) {
        this.topLevelFunction = topLevelFunction;
        this.className = allocateClassName();
    }

    public BatchResult compile() {
        ExpressionTypeInferencer.inferTypesIn(topLevelFunction);
        topLevelFunction.closureImplementations().forEach(ExpressionTypeInferencer::inferTypesIn);
        ExpressionTypeObserver.analyze(topLevelFunction);
        topLevelFunction.closureImplementations().forEach(ExpressionTypeObserver::analyze);
//        NodePrettyPrinter.print(function.body());
        setupClassWriter();
        generateMethodsFor(topLevelFunction);
        for (FunctionImplementation each : topLevelFunction.closureImplementations()) {
            generateMethodsFor(each);
        }
        classWriter.visitEnd();
        return new BatchResult(className, classWriter.toByteArray(), individualResults);
    }

    private void generateMethodsFor(FunctionImplementation function) {
        var methodName = generateGenericMethod(function);
        specializedMethodName = null;
        specializationType = null;
        if (function.profile.canBeSpecialized()) {
            generateSpecializedMethod(function); // sets 'specializedMethodName' and 'specializationType'
        }
        individualResults.put(
            function,
            new FunctionCompilationResult(methodName, specializedMethodName, specializationType));
        generatedMethodSerial++;
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

    private String generateGenericMethod(FunctionImplementation closureImpl) {
        var methodName = GENERIC_METHOD_NAME + generatedMethodSerial;
        MethodVisitor methodWriter = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            methodName,
            MethodType.genericMethodType(closureImpl.implementationArity()).toMethodDescriptorString(),
            null, null);
        methodWriter.visitCode();
        CompilerCodeGeneratorGeneric generator = new CompilerCodeGeneratorGeneric(methodWriter);
        JvmType resultType = generator.generate(closureImpl);
        generator.writer.convertType(resultType, REFERENCE);
        methodWriter.visitInsn(Opcodes.ARETURN);
        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
        return methodName;
    }

    private void generateSpecializedMethod(FunctionImplementation closureImpl) {
        specializedMethodName = SPECIALIZED_METHOD_NAME + generatedMethodSerial; // assuming it's been incremented by generic generator
        specializationType = computeSpecializationType(closureImpl);
        System.out.println("generating a specialized method of type " + specializationType);
        MethodVisitor methodWriter = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            specializedMethodName,
            specializationType.toMethodDescriptorString(),
            null, null);
        methodWriter.visitCode();
        CompilerCodeGeneratorSpecialized generator = new CompilerCodeGeneratorSpecialized(closureImpl, methodWriter);
        generator.generate();
        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
    }

    @NotNull private MethodType computeSpecializationType(FunctionImplementation closureImpl) {
        // Boxed synthetic parameters are passed in as boxes, so they are reference type no matter the var type.
        Stream<JvmType> syntheticParamTypes = closureImpl.syntheticParameters().stream()
            .map(each -> each.isBoxed() ? REFERENCE : each.observedType().jvmType().orElse(REFERENCE));
        // Declared parameters follow their observed type.
        Stream<JvmType> declaredParamTypes = closureImpl.declaredParameters().stream()
            .map(each -> each.observedType().jvmType().orElse(REFERENCE));
        Class<?>[] argClasses = Stream.concat(syntheticParamTypes, declaredParamTypes)
            .map(each -> each.representativeClass())
            .toArray(Class[]::new);
        return MethodType.methodType(
            representativeType(closureImpl.body().observedType()),
            argClasses);
    }

    @SuppressWarnings("unchecked")
    private Class<?> representativeType(ExpressionType observedType) {
        return observedType.jvmType().orElse(REFERENCE).representativeClass();
    }
}
