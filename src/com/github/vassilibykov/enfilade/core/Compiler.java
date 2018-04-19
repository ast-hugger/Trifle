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
 * Compiles a unit consisting of a {@link FunctionImplementation} of a top-level
 * function and function implementations of all its nested closures into a class
 * with <em>implementation methods</em> for all representations of these functions.
 *
 * <h2>Compilation Scheme Overview</h2>
 *
 * <p>A compilation unit consists of a top-level function and functions for each
 * closure nested in it directly or indirectly. A unit is compiled into a JVM
 * class with static <em>implementation methods</em>. Each function is compiled
 * into a minimum of one and a maximum of two implementation methods.
 *
 * <p>A function is always compiled into a <em>generic</em> method. A generic
 * method is a method whose all parameters and return types are Object. The
 * function will also be compiled into a <em>specialized</em> method if during
 * profiling all values of some of the function parameters have fit a narrow
 * primitive type such as {@code int}. A specialized method will have those
 * parameters of the appropriate narrow type, and might have a narrow return
 * type as well, depending on the observations of the function's return values.
 *
 * <p>Each implementation method includes the "normal" implementation code
 * generated from the function's body. Operations performed in the
 * implementation code may be generic (performed on and producing references) or
 * specialized, depending on the observations of the values involved. Note that
 * these internal specializations are independent of whether the method itself
 * is generic or specialized. A generic implementation method may contain
 * specialized operations - for example, if it contains a loop over integers.
 *
 * <p>Specialized operations which implement complex expressions may fail,
 * throwing {@link SquarePegException}s. All such "fallible" operations are
 * compiled so that a potential SPE they throw is caught and handled within the
 * method. Thus, the normal code of a method is in the general case followed
 * by a series of SPE handlers, one for each potential failure site.
 *
 * <p>An SPE handler begins execution with a {@link SquarePegException} instance
 * on the stack. The exception contains a value produced by the complex
 * expression which doesn't fit the specialization of the continuation of that
 * expression. For example, a call to a function from a call site of {@code int}
 * return type will return by throwing an SPE if the return value is a reference
 * not representable as an {@code int} value. Our task at this point is to
 * <em>recover</em> by switching execution to a path which can accept this
 * more generic value.
 *
 * <p>For that, every function implementation method with fallible operations
 * contains recovery code. Recovery code is essentially a version of normal
 * function code, but generated so that all its operations are generic. (So
 * recovery code itself is infallible, that is, can't itself have specialization
 * failures).
 *
 * <p>An SPE handler extracts the continuation value from the exception
 * instance, converts the values of all live locals of primitive types into the
 * corresponding wrapper objects, and then jumps to the location in the recovery
 * code which corresponds to the location in normal code where execution would
 * have continued were it not for the SPE. As a result, execution proceeds as
 * if it were running in the recovery code from the beginning.
 */
class Compiler {

    private static final String GENERIC_METHOD_PREFIX = "function";
    private static final String SPECIALIZED_METHOD_PREFIX = "specialized$";
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final String GENERATED_CODE_PACKAGE = GeneratedCode.class.getPackageName();
    private static final String GENERATED_CLASS_NAME_PREFIX = GENERATED_CODE_PACKAGE + ".$gen$";

    /**
     * The access point: compile a function.
     */
    static UnitResult compile(FunctionImplementation topLevelFunction) {
        Compiler compiler = new Compiler(topLevelFunction);
        UnitResult result = compiler.compile();
        dumpClassFile(result.bytecode());
        return result;
    }

    private static void dumpClassFile(byte[] bytecode) {
        File classFile = new File("generated.class");
        try {
            FileOutputStream classStream = new FileOutputStream(classFile);
            classStream.write(bytecode);
            classStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class UnitResult {
        private byte[] bytecode;
        private final Map<FunctionImplementation, FunctionResult> functionResults = new HashMap<>();

        private UnitResult() {
        }

        byte[] bytecode() {
            return bytecode;
        }

        FunctionResult functionResultFor(FunctionImplementation function) {
            return Objects.requireNonNull(functionResults.get(function));
        }

        Map<FunctionImplementation, FunctionResult> results() {
            return Collections.unmodifiableMap(functionResults);
        }

        private void addFunctionResult(FunctionImplementation function, FunctionResult result) {
            functionResults.put(function, result);
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

    private static String allocateClassName() {
        return GENERATED_CLASS_NAME_PREFIX + serial++;
    }

    /*
        Instance
     */

    private final FunctionImplementation topLevelFunction;
    private final String className;
    private final UnitResult result;
    private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    private int generatedMethodSerial = 0;

    private Compiler(FunctionImplementation topLevelFunction) {
        if (!topLevelFunction.isTopLevel()) throw new AssertionError();
        this.topLevelFunction = topLevelFunction;
        this.className = allocateClassName();
        this.result = new UnitResult();
    }

    public UnitResult compile() {
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
            GhostWriter.internalClassName(className),
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
