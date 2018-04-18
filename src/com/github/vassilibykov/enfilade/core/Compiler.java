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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

/**
 * Compiles a {@link FunctionImplementation} of a top-level function into a
 * class with methods implementing all forms of the top-level function and
 * its nested functions.
 */
public class Compiler {

    static final MethodType RECOVERY_METHOD_TYPE = MethodType.methodType(
        Object.class, Object.class, int.class, Object[].class);

    private static final String GENERIC_METHOD_PREFIX = "function";
    private static final String SPECIALIZED_METHOD_PREFIX = "specialized$";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final String GENERATED_CODE_PACKAGE = "com.github.vassilibykov.enfilade.core";
    private static final String GENERATED_CLASS_NAME_PREFIX = GENERATED_CODE_PACKAGE + ".$gen$";

    /**
     * The access point: compile a function.
     */
    public static Result compile(FunctionImplementation topLevelFunction) {
        Compiler compiler = new Compiler(topLevelFunction);
        Result result = compiler.compile();
        dumpClassFile("generated", result.bytecode());
        return result;
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

    static class Result {
        private final String className;
        private byte[] bytecode;
        private final Map<FunctionImplementation, FunctionResult> results = new HashMap<>();

        private Result(String className) {
            this.className = className;
        }

        String className() {
            return className;
        }

        byte[] bytecode() {
            return bytecode;
        }

        FunctionResult functionResultFor(FunctionImplementation function) {
            return Objects.requireNonNull(results.get(function));
        }

        Map<FunctionImplementation, FunctionResult> results() {
            return Collections.unmodifiableMap(results);
        }

        private void addFunctionResult(FunctionImplementation function, FunctionResult result) {
            results.put(function, result);
        }

        private void setBytecode(byte[] bytecode) {
            this.bytecode = bytecode;
        }
    }

    static class FunctionResult {
        @NotNull private final String genericMethodName;
        @Nullable private String specializedMethodName;
        @Nullable private MethodType specializedMethodType;

        FunctionResult(@NotNull String genericMethodName)
        {
            this.genericMethodName = genericMethodName;
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

    private static long serial = 0;

    static String internalClassName(Class<?> klass) {
        return internalClassName(klass.getName());
    }

    static String internalClassName(String fqnName) {
        return fqnName.replace('.', '/');
    }

    static String allocateClassName() {
        return GENERATED_CLASS_NAME_PREFIX + serial++;
    }

    /*
        Instance
     */

    private final FunctionImplementation topLevelFunction;
    private final String className;
    private final Result result;
    private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    private int generatedMethodSerial = 0;

    private Compiler(FunctionImplementation topLevelFunction) {
        this.topLevelFunction = topLevelFunction;
        this.className = allocateClassName();
        this.result = new Result(this.className);
    }

    public Result compile() {
        inferTypes();
        setupClassWriter();
        generateGenericMethods();
        generateSpecializedMethods();
        classWriter.visitEnd();
        result.setBytecode(classWriter.toByteArray());
        return result;
    }

    private void inferTypes() {
        ExpressionTypeInferencer.inferTypesIn(topLevelFunction);
        topLevelFunction.closureImplementations().forEach(ExpressionTypeInferencer::inferTypesIn);
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

    private void generateGenericMethods() {
        SpecializedTypeComputer.process(true, topLevelFunction);
        generateGenericMethodFor(topLevelFunction);
        topLevelFunction.closureImplementations().forEach(this::generateGenericMethodFor);
    }

    private void generateGenericMethodFor(FunctionImplementation function) {
        var methodName = generateGenericMethod(function);
        var functionResult = new FunctionResult(methodName);
        result.addFunctionResult(function, functionResult);
        generatedMethodSerial++;
    }

    private void generateSpecializedMethods() {
        SpecializedTypeComputer.process(false, topLevelFunction);
        generateSpecializedMethodFor(topLevelFunction, result.functionResultFor(topLevelFunction));
        topLevelFunction.closureImplementations().forEach(
            each -> generateSpecializedMethodFor(each, result.functionResultFor(each)));
    }

    private void generateSpecializedMethodFor(FunctionImplementation function, FunctionResult functionResult) {
        if (function.canBeSpecialized()) {
            generateSpecializedMethod(function, functionResult);
        }
    }

    private String generateGenericMethod(FunctionImplementation closureImpl) {
        var methodName = GENERIC_METHOD_PREFIX + generatedMethodSerial;
        MethodVisitor methodWriter = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            methodName,
            MethodType.genericMethodType(closureImpl.implementationArity()).toMethodDescriptorString(),
            null, null);
        methodWriter.visitCode();
        var generator = new MethodCodeGenerator(closureImpl, methodWriter);
        generator.generate();
        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
        return methodName;
    }

    private void generateSpecializedMethod(FunctionImplementation closureImpl, FunctionResult functionResult) {
        var methodName = SPECIALIZED_METHOD_PREFIX + functionResult.genericMethodName;
        var methodType = computeSpecializationType(closureImpl);
        System.out.println("generating a specialized method of type " + methodType);
        MethodVisitor methodWriter = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            methodName,
            methodType.toMethodDescriptorString(),
            null, null);
        methodWriter.visitCode();
        var generator = new MethodCodeGenerator(closureImpl, methodWriter);
        generator.generate();
        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
        functionResult.specializedMethodName = methodName;
        functionResult.specializedMethodType = methodType;
    }

    @NotNull private MethodType computeSpecializationType(FunctionImplementation function) {
        // Boxed synthetic parameters are passed in as boxes, so they are reference type no matter the var type.
        Stream<JvmType> syntheticParamTypes = function.syntheticParameters().stream()
            .map(each -> each.isBoxed() ? REFERENCE : each.specializedType());
        // Declared parameters follow their observed type.
        Stream<JvmType> declaredParamTypes = function.declaredParameters().stream()
            .map(each -> each.specializedType());
        Class<?>[] argClasses = Stream.concat(syntheticParamTypes, declaredParamTypes)
            .map(each -> each.representativeClass())
            .toArray(Class[]::new);
        return MethodType.methodType(
            function.specializedReturnType.representativeClass(),
            argClasses);
    }
}
