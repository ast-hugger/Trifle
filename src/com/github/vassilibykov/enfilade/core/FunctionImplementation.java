// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An object holding together all executable representations of a function
 * (though not necessarily all of them are available at any given time). The
 * representations are: a tree of {@link EvaluatorNode}s, a list of recovery
 * interpreter instructions, method handles to invoke generic and specialized
 * compiled representations. This is <em>not</em> a function value in the
 * implemented language. For that, see {@link Closure}.
 *
 * <p>Each function implementation object corresponds to a lambda expression in
 * the source language. Thus, a top-level function with two nested closures
 * would map onto three {@link FunctionImplementation}s. A {@link Closure}
 * instance created when a lambda expression is evaluated references the
 * corresponding function implementation.
 *
 * <p>At the core of a function implementation's invocation mechanism is its
 * {@link #callSite}, referred to as "the core call site", and an invoker of
 * that call site stored in the {@link #callSiteInvoker} field. The target of
 * that call site is a method handle invoking which will execute the function
 * using the best currently available option. For a newly created function
 * implementation that would be execution by a profiling interpreter. Later the
 * target of the call site is changed to a faster non-profiling interpreter
 * while the function is being compiled, and eventually to a method handle to
 * the generic compiled form of the function.
 *
 * <p>In a top-level function with {@code n} declared parameters, the core call
 * site has the type
 *
 * <pre>{@code
 * (Object{n}) -> Object
 * }</pre>

 * <p>A non-top-level function may have additional synthetic parameters
 * prepended by the closure conversion process. The core call site of a function
 * with {@code k} parameters introduced by the closure converter has the type
 *
 * <pre>{@code
 * (Object{k} Object{n}) -> Object
 * }</pre>
 *
 * <p>When invoked by a standard {@code call} expression with a closure as the
 * function argument, executed by the interpreter, invocation is kicked off by
 * one of the {@link Closure#invoke} methods, receiving the call arguments
 * ({@code n} Objects).
 *
 * <p>When the same expression is executed by generic compiled code, the call
 * site in the caller has the signature
 *
 * <pre>{@code
 * (Object Object{n}) -> Object
 * }</pre>
 *
 * <p>Note the extra leading argument. It contains the closure being called, but
 * is formally typed as {@code Object} rather than closure. Internally a closure
 * maintains an {@link Closure#defaultInvoker} method handle which calls its
 * function implementation's {@link #callSiteInvoker} after inserting copied
 * values, if any, to be received by the synthetic parameters prepended by the
 * closure converter.
 *
 * <p>In addition to the generic compiled form bound to the core {@link
 * #callSite}, a function implementation may have a specialized compiled form. A
 * specialized form is produced by the compiler if the profiling interpreter
 * observed at least one of the function arguments to always be of a primitive
 * type. In a specialized form is available, the {@link
 * #specializedImplementation} field is not null. It contains a method handle of
 * the specialized compiled form. The method does NOT have the leading closure
 * parameter.
 *
 * <p>There are two mechanisms of how a specialized implementation can be
 * invoked. One is from the "normal" generic invocation pipeline, which includes
 * both the interpreted and the compiled generic cases. If a function has a
 * specialization, the method handle of its core {@link #callSite} is a guard
 * testing the current arguments for applicability to the specialized form. For
 * example, if a unary function has an {@code (int)} specialization, the guard
 * would test the invocation argument for being an {@code Integer}. Depending on
 * the result of the test, either the specialized or the generic form is
 * invoked.
 *
 * <p>The other mechanism is an invocation from specialized code. The
 * specialized code compiler generates a call site with the signature matching
 * the specialized types of arguments. A binary call with both arguments
 * specialized as {@code int} and return typed observed to be {@code int} has
 * its call site typed as {@code (Object int int) -> int} (the leading argument
 * is again the closure typed as Object). The same function might be called
 * elsewhere from a call site typed as {@code (Object int Object) -> Object} if
 * those were the types observed at that call site.
 *
 * <p>--TBD-- I need to better think through how {@link Closure} and {@link
 * ClosureInvokeDynamic} efficiently can bind to specialized forms.
 */
public class FunctionImplementation {

    /** The number of times a function is profiled before it's queued for compilation. */
    private static final long PROFILING_TARGET = 100; // Long.MAX_VALUE;

    private enum State {
        INVALID,
        PROFILING,
        COMPILING,
        COMPILED
    }

    /*
        Instance
     */

    @NotNull private final Lambda definition;
    /**
     * For an implementation of non-top level lambda expression, contains the
     * implementation of the topmost lambda expression containing this one.
     * For an implementation of the top-leve expression, contains this function
     * implementation.
     */
    @NotNull private final FunctionImplementation topImplementation;
    /**
     * Apparent parameters from the function definition. Does not include synthetic
     * parameters introduced by closure conversion to copy down free variables.
     */
    private List<VariableDefinition> declaredParameters;
    /**
     * All copied variables created during closure conversion.
     */
    private List<CopiedVariable> syntheticParameters;
    /**
     * The actual parameter list, beginning with synthetic parameters for copied down
     * values followed by apparent parameters.
     */
    private AbstractVariable[] allParameters;
    /**
     * In a top-level function, contains function implementations of closures defined in
     * it. Empty in non-toplevel functions even if they do contain closures. Functions
     * are listed in tree traversal encounter order, so they are topologically sorted
     * with respect to their nesting.
     */
    private final List<FunctionImplementation> closureImplementations = new ArrayList<>();
    private final int arity;
    private EvaluatorNode body;
    private int frameSize = -1;
    private List<RecoverySite> recoverySites;
    /*internal*/ FunctionProfile profile;
    /**
     * The unique ID of the function in the function registry.
     */
    private int id = -1;
    /**
     * Nesting depth of the function, with the top-level function having the depth of 0.
     */
    /*internal*/ int depth = -1;
    /**
     * A call site invoking which will execute the function using the currently
     * appropriate execution mode (profiled vs compiled). The call site has the
     * generic signature of {@code (Closure Object{n}) -> Object}.
     */
    private MutableCallSite callSite;
    /**
     * The dynamic invoker of {@link #callSite}.
     */
    /*internal*/ MethodHandle callSiteInvoker;
    /*internal*/ MethodHandle genericImplementation;
    /*internal*/ MethodHandle specializedImplementation;
    /*internal*/ MethodHandle recoveryImplementation;
    private volatile State state;

    FunctionImplementation(@NotNull Lambda definition, @Nullable FunctionImplementation topImplOrNull) {
        this.definition = definition;
        this.topImplementation = topImplOrNull != null ? topImplOrNull : this;
        this.arity = definition.arguments().size();
        this.state = State.INVALID;
    }

    /** RESTRICTED. Intended for {@link FunctionTranslator}. */
    void partiallyInitialize(@NotNull List<VariableDefinition> parameters, @NotNull EvaluatorNode body) {
        this.declaredParameters = parameters;
        this.profile = new FunctionProfile(parameters);
        this.body = body;
    }

    void addClosureImplementations(Collection<FunctionImplementation> functions) {
        closureImplementations.addAll(functions);
    }

    /** RESTRICTED. Intended for {@link FunctionAnalyzer.Indexer}. */
    void finishInitialization(int frameSize, List<RecoverySite> recoverySites) {
        this.callSite = new MutableCallSite(profilingInterpreterInvoker());
//        this.callSite = new MutableCallSite(simpleInterpreterInvoker());
        this.callSiteInvoker = callSite.dynamicInvoker();
        this.frameSize = frameSize;
        this.recoverySites = Collections.unmodifiableList(recoverySites);
        this.state = State.PROFILING;
    }

    public Lambda definition() {
        return definition;
    }

    public int id() {
        return id;
    }

    /** RESTRICTED. Intended for {@link FunctionRegistry#lookup(FunctionImplementation)}. */
    void setId(int id) {
        this.id = id;
    }

    public List<VariableDefinition> declaredParameters() {
        return declaredParameters;
    }

    public List<CopiedVariable> syntheticParameters() {
        return syntheticParameters;
    }

    public AbstractVariable[] allParameters() {
        return allParameters;
    }

    public List<FunctionImplementation> closureImplementations() {
        return closureImplementations;
    }

    /**
     * RESTRICTED. Intended for {@link FunctionAnalyzer.ClosureConverter}.
     * Accept the synthetic variables into which free variable references have been
     * rewritten in this function.
     */
    /*internal*/ void setSyntheticParameters(Collection<CopiedVariable> variables) {
        this.syntheticParameters = new ArrayList<>(variables);
        this.allParameters = Stream.concat(syntheticParameters.stream(), declaredParameters.stream())
            .toArray(AbstractVariable[]::new);
    }

    public EvaluatorNode body() {
        return body;
    }

    /**
     * Return the arity of the underlying abstract definition (before closure conversion).
     */
    public int declarationArity() {
        return arity;
    }

    /**
     * Return the arity of the closure-converted function, which includes both
     * synthetic and declared parameters.
     */
    public int implementationArity() {
        return allParameters.length;
    }

    public int frameSize() {
        return frameSize;
    }

    public List<RecoverySite> recoverySites() {
        return recoverySites;
    }

    public boolean isCompiled() {
        return state == State.COMPILED;
    }

    private MethodHandle profilingInterpreterInvoker() {
        var type = MethodType.genericMethodType(implementationArity());
        try {
            var handle = MethodHandles.lookup().findVirtual(FunctionImplementation.class, "profile", type);
            return handle.bindTo(this);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private MethodHandle simpleInterpreterInvoker() {
        var type = MethodType.genericMethodType(implementationArity());
        type = type.insertParameterTypes(0, FunctionImplementation.class);
        try {
            MethodHandle interpret = MethodHandles.lookup().findVirtual(Interpreter.class, "interpret", type);
            return MethodHandles.insertArguments(interpret, 0, Interpreter.INSTANCE, this);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public Object profile() {
        Object result = ProfilingInterpreter.INSTANCE.interpret(this);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object profile(Object arg) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(this, arg);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object profile(Object arg1, Object arg2) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(this, arg1, arg2);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object profile(Object arg1, Object arg2, Object arg3) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(this, arg1, arg2, arg3);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    private void scheduleCompilation() {
        topImplementation.scheduleCompilationAtTop();
    }

    private synchronized void scheduleCompilationAtTop() {
        if (state == State.PROFILING) {
            markAsBeingCompiled();
            for (var each : closureImplementations) each.markAsBeingCompiled();
            forceCompile();
        }
    }

    private void markAsBeingCompiled() {
        state = State.COMPILING;
        callSite.setTarget(simpleInterpreterInvoker());
    }

    void forceCompile() {
        if (this != topImplementation) throw new AssertionError("must be invoked on a top function implementation");
        var result = Compiler.compile(this);
        applyCompilationResult(result);
    }

    private synchronized void applyCompilationResult(Compiler.BatchResult batchResult) {
        var implClass = GeneratedCode.defineClass(batchResult);
        var callSitesToUpdate = new ArrayList<MutableCallSite>();
        for (var entry : batchResult.results().entrySet()) {
            var functionImpl = entry.getKey();
            functionImpl.updateCompiledForm(implClass, entry.getValue());
            callSitesToUpdate.add(functionImpl.callSite);
        }
        MutableCallSite.syncAll(callSitesToUpdate.toArray(new MutableCallSite[0]));
    }

    private void updateCompiledForm(Class<?> generatedClass, Compiler.FunctionCompilationResult result) {
        MethodHandle specializedMethod = null;
        try {
            genericImplementation = MethodHandles.lookup()
                .findStatic(generatedClass, result.genericMethodName(), MethodType.genericMethodType(implementationArity()));
            if (result.specializedMethodName() != null) {
                specializedMethod = MethodHandles.lookup()
                    .findStatic(generatedClass, result.specializedMethodName(), result.specializedMethodType());
            }
            if (result.recoveryMethodName() != null) {
                recoveryImplementation = MethodHandles.lookup()
                    .findStatic(generatedClass, result.recoveryMethodName(), Compiler.RECOVERY_METHOD_TYPE);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
        state = State.COMPILED;
        if (specializedMethod == null) {
            callSite.setTarget(genericImplementation);
            specializedImplementation = null;
            // this will not work if we allow de-specializing
        } else {
            callSite.setTarget(
                    makeSpecializationGuard(genericImplementation, specializedMethod, result.specializedMethodType()));
            specializedImplementation = specializedMethod;
        }
    }

    private MethodHandle makeSpecializationGuard(MethodHandle generic, MethodHandle specialized, MethodType type) {
        MethodHandle checker = CHECK.bindTo(type);
        return MethodHandles.guardWithTest(
            checker.asCollector(Object[].class, type.parameterCount()),
            generify(specialized),
            generic);
    }

    /**
     * Take a method handle of a type involving some primitive types and wrap it
     * so that it accepts and returns all Objects.
     */
    private MethodHandle generify(MethodHandle specialization) {
        MethodHandle generic = specialization.asType(specialization.type().generic());
        // If return type is primitive, a return value not fitting the type will come back as SPE
        if (specialization.type().returnType().isPrimitive()) {
            return MethodHandles.catchException(generic, SquarePegException.class, EXTRACT_SQUARE_PEG);
        } else {
            return generic;
        }
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean checkSpecializationApplicability(MethodType specialization, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            Class<?> type = specialization.parameterType(i);
            if (type.isPrimitive()) {
                Object arg = args[i];
                if (!JvmType.isCompatibleValue(type, arg)) return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused") // called by generated code
    public static Object extractSquarePeg(SquarePegException exception) {
        return exception.value;
    }

    private static final MethodHandle CHECK;
    private static final MethodHandle EXTRACT_SQUARE_PEG;

    static {
        try {
            CHECK = MethodHandles.lookup().findStatic(
                FunctionImplementation.class,
                "checkSpecializationApplicability",
                MethodType.methodType(boolean.class, MethodType.class, Object[].class));
            EXTRACT_SQUARE_PEG = MethodHandles.lookup().findStatic(
                FunctionImplementation.class,
                "extractSquarePeg", MethodType.methodType(Object.class, SquarePegException.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() {
        return "[" + depth + "] " + definition.toString();
    }
}
