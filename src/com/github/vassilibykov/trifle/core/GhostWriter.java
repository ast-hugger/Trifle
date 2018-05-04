// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
        return internalClassNameCache.computeIfAbsent(klass, k -> internalClassName(k.getName()));
    }
    private static final Map<Class<?>, String> internalClassNameCache = new ConcurrentHashMap<>();

    public static String internalClassName(String fqnName) {
        return fqnName.replace('.', '/');
    }

    public static String methodDescriptor(Class<?> returnType, Class<?>... argTypes) {
        switch (argTypes.length) {
            case 0:
                return nullaryMethodDescriptorCache.computeIfAbsent(returnType,
                    c -> MethodType.methodType(returnType).toMethodDescriptorString());
            case 1:
                var argType = argTypes[0];
                var mapByArg = unaryMethodDescriptorCache.computeIfAbsent(returnType,
                    c -> new ConcurrentHashMap<>());
                return mapByArg.computeIfAbsent(argType,
                    c -> MethodType.methodType(returnType, argType).toMethodDescriptorString());
            default:
                return MethodType.methodType(returnType, argTypes).toMethodDescriptorString();
        }
    }
    private static final Map<Class<?>, String> nullaryMethodDescriptorCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<Class<?>, String>> unaryMethodDescriptorCache = new ConcurrentHashMap<>();

    private static final String OBJECT_DESC = "Ljava/lang/Object;";
    private static final String INTEGER_ICN = internalClassName(Integer.class);
    private static final String BOOLEAN_ICN = internalClassName(Boolean.class);

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

    public GhostWriter adaptValue(JvmType from, JvmType to) {
        from.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { unwrapInteger(); }
                    public void ifBoolean() { unwrapBoolean(); }
                });
            }
            public void ifInt() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { wrapInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { throw new CompilerError("cannot adapt int to boolean"); }
                });
            }
            public void ifBoolean() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { wrapBoolean(); }
                    public void ifInt() {
                        throw new CompilerError("cannot adapt boolean to int");
                    }
                    public void ifBoolean() { }
                });
            }
            public void ifVoid() {
                // means the computation that produced the value terminated the current invocation
            }
        });
        return this;
    }

    /**
     * Assuming that a value of type 'from' is on the stack in the context whose
     * continuation expects a value of type 'to', generate code that will ensure
     * the continuation will successfully receive the value.
     *
     * <p>If the from/to pair of types is such that a value of 'from' cannot in
     * the general case be converted to a value of 'to', for example
     * {@code reference -> int} when the reference is not to an {@code Integer},
     * the generated code may throw an exception to complete the evaluation in
     * recovery mode.
     *
     * <p>If the 'to' type is VOID, that means the value will be discarded by
     * the continuation, so it doesn't matter what it is.
     *
     * <p>This is different from {@link GhostWriter#adaptValue(JvmType,
     * JvmType)}. The latter performs wrapping and unwrapping of values,
     * assuming that in a conversion between a primitive and a reference type,
     * the reference type is a valid wrapper value for the primitive. When
     * adapting a reference to an int, the reference can value never be
     * anything other than {@code Integer}. This is true no matter if the user
     * program is correct or not. A violation of this expectation is a sign of
     * an internal error in the compiler.
     *
     * <p>In contrast, in bridging a reference to an int it's normal for the
     * reference value to not be an {@code Integer}. In that case it should be
     * packaged up and thrown as a {@link SquarePegException}.
     *
     * @param from The type initially on the stack.
     * @param to The type required to be on the stack.
     * @return An indication of whether a {@link SquarePegException} can be
     *         thrown in the generated code.
     */
    public boolean bridgeValue(JvmType from, JvmType to) {
        return from.match(new JvmType.Matcher<>() {
            public Boolean ifReference() {
                return to.match(new JvmType.Matcher<>() {
                    public Boolean ifReference() {
                        return false;
                    }
                    public Boolean ifInt() {
                        unwrapIntegerOr(GhostWriter.this::throwSquarePegException);
                        return true;
                    }
                    public Boolean ifBoolean() {
                        unwrapBooleanOr(GhostWriter.this::throwSquarePegException);
                        return true;
                    }
                    public Boolean ifVoid() {
                        return false;
                    }
                });
            }
            public Boolean ifInt() {
                return to.match(new JvmType.Matcher<>() {
                    public Boolean ifReference() {
                        wrapInteger();
                        return false;
                    }
                    public Boolean ifInt() {
                        return false;
                    }
                    public Boolean ifBoolean() {
                        wrapInteger().throwSquarePegException();
                        return true;
                    }
                    public Boolean ifVoid() {
                        return false;
                    }
                });
            }
            public Boolean ifBoolean() {
                return to.match(new JvmType.Matcher<>() {
                    public Boolean ifReference() {
                        wrapBoolean();
                        return false;
                    }
                    public Boolean ifInt() {
                        wrapBoolean().throwSquarePegException();
                        return true;
                    }
                    public Boolean ifBoolean() {
                        return false;
                    }
                    public Boolean ifVoid() {
                        return false;
                    }
                });
            }
            public Boolean ifVoid() {
                // occurs in the middle of blocks and in return statements; nothing needs to be done
                return false;
            }
        });
    }


    public GhostWriter ensureValue(JvmType from, JvmType to) {
        from.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { unwrapIntegerOr(() -> throwIntegerExpected()); }
                    public void ifBoolean() { unwrapBooleanOr(() -> throwBooleanExpected()); }
                });
            }
            public void ifInt() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { wrapInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { throw new CompilerError("cannot convert int to boolean"); }
                });
            }
            public void ifBoolean() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { wrapBoolean(); }
                    public void ifInt() {
                        throw new CompilerError("cannot convert boolean to int");
                    }
                    public void ifBoolean() { }
                });
            }
        });
        return this;
    }

    public GhostWriter wrapBoolean() {
        invokeStatic(Boolean.class, "valueOf", Boolean.class, boolean.class);
        return this;
    }

    public GhostWriter wrapInteger() {
        invokeStatic(Integer.class, "valueOf", Integer.class, int.class);
        return this;
    }

    public GhostWriter checkCast(Class<?> castClass) {
        asmWriter.visitTypeInsn(CHECKCAST, internalClassName(castClass));
        return this;
    }

    public GhostWriter checkCast(String internalClassName) {
        asmWriter.visitTypeInsn(CHECKCAST, internalClassName);
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
        checkCast(Box.class);
        invokeVirtual(Box.class, Box.VALUE_AS_REFERENCE, Object.class);
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
        invokeStatic(Box.class, "with", Box.class, Object.class);
        asmWriter.visitVarInsn(ASTORE, index);
        return this;
    }

    public GhostWriter initBoxedBool(int index) {
        wrapBoolean();
        invokeStatic(Box.class, "with", Box.class, Object.class);
        asmWriter.visitVarInsn(ASTORE, index);
        return this;
    }

    public GhostWriter initBoxedInt(int index) {
        invokeStatic(Box.class, "with", Box.class, int.class);
        asmWriter.visitVarInsn(ASTORE, index);
        return this;
    }

    public GhostWriter instanceOf(Class<?> targetClass) {
        asmWriter.visitTypeInsn(INSTANCEOF, internalClassName(targetClass));
        return this;
    }

    public GhostWriter instanceOf(String internalClassName) {
        asmWriter.visitTypeInsn(INSTANCEOF, internalClassName);
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
            methodDescriptor(returnType, argTypes),
            false);
        return this;
    }

    public GhostWriter invokeVirtual(Class<?> owner, String methodName, Class<?> returnType, Class<?>... argTypes) {
        invokeVirtual(internalClassName(owner), methodName, returnType, argTypes);
        return this;
    }

    public GhostWriter invokeVirtual(String ownerClassName, String methodName, Class<?> returnType, Class<?>... argTypes) {
        asmWriter.visitMethodInsn(
            INVOKEVIRTUAL,
            ownerClassName,
            methodName,
            methodDescriptor(returnType, argTypes),
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

    public GhostWriter jumpIfNot0(Label label) {
        asmWriter.visitJumpInsn(IFNE, label);
        return this;
    }

    public GhostWriter loadClass(Class<?> klass) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
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

    public GhostWriter ret(JvmType type) {
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() { asmWriter.visitInsn(ARETURN); }
            public void ifInt() { asmWriter.visitInsn(IRETURN); }
            public void ifBoolean() { asmWriter.visitInsn(IRETURN); }
        });
        return this;
    }

    public GhostWriter setLabelHere(Label label) {
        asmWriter.visitLabel(label);
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
        checkCast(Box.class);
        swap();
        invokeVirtual(Box.class, Box.SET_VALUE, void.class, Object.class);
        return this;
    }

    public GhostWriter storeBoxedBool(int index) {
        asmWriter.visitVarInsn(ALOAD, index);
        checkCast(Box.class);
        swap();
        wrapBoolean();
        invokeVirtual(Box.class, Box.SET_VALUE, void.class, Object.class);
        return this;
    }

    public GhostWriter storeBoxedInt(int index) {
        asmWriter.visitVarInsn(ALOAD, index);
        checkCast(Box.class);
        swap();
        invokeVirtual(Box.class, Box.SET_VALUE, void.class, int.class);
        return this;
    }

    public GhostWriter swap() {
        asmWriter.visitInsn(SWAP);
        return this;
    }

    public GhostWriter throwBooleanExpected() {
        invokeStatic(RuntimeError.class, "booleanExpected", RuntimeError.class);
        asmWriter.visitInsn(ATHROW);
        return this;
    }

    public GhostWriter throwError(String message) {
        loadString(message);
        invokeStatic(RuntimeError.class, "message", RuntimeError.class, String.class);
        asmWriter.visitInsn(ATHROW);
        return this;
    }

    public GhostWriter throwIntegerExpected() {
        invokeStatic(RuntimeError.class, "integerExpected", RuntimeError.class);
        asmWriter.visitInsn(ATHROW);
        return this;
    }

    public GhostWriter throwSquarePegException() {
        invokeStatic(SquarePegException.class, "with", SquarePegException.class, Object.class);
        asmWriter.visitInsn(ATHROW);
        return this;
    }

    /**
     * Generate code to unbox the value currently on the stack, which must be a
     * box of the appropriate type. This is not unboxing in the Java sense
     * between a wrapper and a primitive type. To avoid confusion we refer to
     * the latter as <em>unwrapping</em>.
     */
    public GhostWriter unboxValue(JvmType type) {
        checkCast(Box.class);
        type.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                invokeVirtual(Box.class, Box.VALUE_AS_REFERENCE, Object.class);
            }
            public void ifInt() {
                invokeVirtual(Box.class, Box.VALUE_AS_INT, int.class);
            }
            public void ifBoolean() {
                invokeVirtual(Box.class, Box.VALUE_AS_REFERENCE, Object.class);
                unwrapBoolean();
            }
        });
        return this;
    }

    public GhostWriter unwrapBoolean() {
        checkCast(Boolean.class);
        invokeVirtual(Boolean.class, "booleanValue", boolean.class);
        return this;
    }

    public GhostWriter unwrapBooleanOr(Runnable failureCodeGenerator) {
        dup();
        instanceOf(BOOLEAN_ICN);
        ifThenElse(
            () -> {
                checkCast(BOOLEAN_ICN);
                invokeVirtual(BOOLEAN_ICN, "booleanValue", boolean.class);
            },
            failureCodeGenerator
        );
        return this;
    }

    public GhostWriter unwrapInteger() {
        checkCast(Integer.class);
        invokeVirtual(Integer.class, "intValue", int.class);
        return this;
    }

    public GhostWriter unwrapIntegerOr(Runnable failureCodeGenerator) {
        dup();
        instanceOf(INTEGER_ICN);
        ifThenElse(
            () -> {
                checkCast(INTEGER_ICN);
                invokeVirtual(INTEGER_ICN, "intValue", int.class);
            },
            failureCodeGenerator
        );
        return this;
    }

    public GhostWriter unwrapSPE() {
        asmWriter.visitFieldInsn(GETFIELD, internalClassName(SquarePegException.class), "value", OBJECT_DESC);
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
