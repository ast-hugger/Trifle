package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodType;

/**
 * Compiles a {@link Function} into a class with a single static method.
 */
public class Compiler {

    public static final String IMPL_METHOD_NAME = "run";

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

        private Result(String className, byte[] bytecode) {
            this.className = className;
            this.bytecode = bytecode;
        }

        public String className() {
            return className;
        }

        public byte[] bytecode() {
            return bytecode;
        }
    }

    static String internalClassName(Class<?> klass) {
        return internalClassName(klass.getName());
    }

    static String internalClassName(String fqnName) {
        return fqnName.replace('.', '/');
    }

    static String allocateClassName() {
        return "$g$" + serial++;
    }

    private static long serial = 0;

    /*
        Instance
     */

    private final Function function;
    private final String className;
    private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    private Compiler(Function function) {
        this.function = function;
        this.className = allocateClassName();
    }

    public Result compile() {
        ValueAnalyzer.analyze(function);
        setupClassWriter();
        generateGenericMethod();
        if (function.profile.canBeSpecialized()) {
            generateSpecializedMethod();
        }
        classWriter.visitEnd();
        return new Result(className, classWriter.toByteArray());
    }

    private void setupClassWriter() {
        classWriter.visit(
            Opcodes.V9,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL,
            internalClassName(className),
            null,
            "java/lang/Object",
            null);
    }

    private void generateGenericMethod() {
        MethodVisitor methodWriter = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            IMPL_METHOD_NAME,
            methodDescriptor(function.arity()),
            null, null);
        methodWriter.visitCode();
        FunctionCodeGenerator functionCodeGenerator = new FunctionCodeGenerator(methodWriter);
        function.body().accept(functionCodeGenerator);
        methodWriter.visitInsn(Opcodes.ARETURN);
        methodWriter.visitMaxs(-1, -1);
        methodWriter.visitEnd();
    }

    private void generateSpecializedMethod() {
        // TODO
    }

    private String methodDescriptor(int arity) {
        return MethodType.genericMethodType(arity).toMethodDescriptorString();
    }
}
