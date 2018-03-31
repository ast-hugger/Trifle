// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Lambda;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VolatileCallSite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * An object holding together all executable representations of a source function (though
 * not necessarily all of them are available at any given time). The representations are:
 * a tree of {@link EvaluatorNode}s, a list of recovery interpreter instructions, method
 * handles to invoke generic and specialzied compiled representations.
 *
 * <p>This is <em>not</em> a function value in the implemented language. For that, see
 * {@link Closure}.
 */
public class FunctionImplementation {

    /** The number of times a function is profiled before it's queued for compilation. */
    private static final long PROFILING_TARGET = 100; // Long.MAX_VALUE;

    private enum State {
        INVALID,
        /**
         * The function is currently running interpreted and collecting type information.
         */
        PROFILING,
        /**
         * The function has finished collecting type information and is
         * scheduled for compilation, however compilation has not yet completed.
         * The {@link #callSite} is bound to one of the {@code interpret()}
         * methods of (the non-profiling) {@code Interpreter.INSTANCE}, so type
         * information is no longer being collected.
         */
        COMPILING,
        /**
         * A compiled representation has been computed for this function.
         * The {@link #callSite} is pointing at the compilation result.
         */
        COMPILED
    }

    /*
        Instance
     */

    @NotNull private final Lambda definition;
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
    private int localsCount = -1;
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
     * appropriate execution mode (profiled vs compiled). The call site has a
     * generic signature of {@code (Closure Object*) -> Object}.
     */
    private VolatileCallSite callSite;
    /**
     * The dynamic invoker of {@link #callSite}.
     */
    /*internal*/ MethodHandle callSiteInvoker;
    @Nullable private VolatileCallSite specializationCallSite;
    /*internal*/ ACodeInstruction[] acode;
    private State state;

    FunctionImplementation(@NotNull Lambda definition) {
        this.definition = definition;
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

    /** RESTRICTED. Intended for {@link FunctionAnalyzer.VariableIndexer}. */
    void finishInitialization(int localsCount) {
        this.callSite = new VolatileCallSite(profilingInterpreterInvoker());
//        this.callSite = new VolatileCallSite(simpleInterpreterInvoker());
//        this.callSite = new VolatileCallSite(acodeInterpreterInvoker());
        this.callSiteInvoker = callSite.dynamicInvoker();
        this.localsCount = localsCount;
//        this.acode = ACodeTranslator.translate(body);
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
     * RESTRICTED. Intended for {@link FunctionAnalyzer.VariableIndexer}.
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
    public int definitionArity() {
        return arity;
    }

    /**
     * Return the arity of the closure-converted function.
     */
    public int implementationArity() {
        return allParameters.length;
    }

    public int frameSize() {
        return localsCount;
    }

    public ACodeInstruction[] acode() {
        return acode;
    }

    /**
     * Return a call site invoking which executes the function in the best
     * currently possible mode. Invokedynamics calling this function all share
     * this call site. The type of the call site matches the arity of the
     * function, so invokedynamics with an incompatible signature will fail at
     * the bootstrap time unless special measures are taken by the bootstrapper.
     */
    public CallSite callSite(MethodType requestedType) {
        if (specializationCallSite != null && requestedType.equals(specializationCallSite.type())) {
            return specializationCallSite;
        } else if (requestedType.equals(callSite.type())){
            return callSite;
        } else {
            throw new UnsupportedOperationException("partial specialization is not yet supported");
        }
    }

    private MethodHandle profilingInterpreterInvoker() {
        var type = MethodType.genericMethodType(implementationArity());
        type = type.insertParameterTypes(0, Closure.class);
        try {
            var handle = MethodHandles.lookup().findVirtual(FunctionImplementation.class, "profile", type);
            return handle.bindTo(this);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private MethodHandle simpleInterpreterInvoker() {
        var type = MethodType.genericMethodType(implementationArity());
        type = type.insertParameterTypes(0, Closure.class);
        try {
            MethodHandle interpret = MethodHandles.lookup().findVirtual(Interpreter.class, "interpret", type);
            return MethodHandles.insertArguments(interpret, 0, Interpreter.INSTANCE);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private MethodHandle acodeInterpreterInvoker() {
        var type = MethodType.genericMethodType(implementationArity());
        type = type.insertParameterTypes(0, Closure.class);
        try {
            MethodHandle interpret = MethodHandles.lookup().findStatic(ACodeInterpreter.class, "interpret", type);
            return MethodHandles.insertArguments(interpret, 0, this);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public Object profile(Closure closure) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(closure);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object profile(Closure closure, Object arg) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(closure, arg);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object profile(Closure closure, Object arg1, Object arg2) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(closure, arg1, arg2);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object profile(Closure closure, Object arg1, Object arg2, Object arg3) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(closure, arg1, arg2, arg3);
        if (profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    private MethodHandle dropFunctionArgument(MethodHandle original) {
        return MethodHandles.dropArguments(original, 0, Closure.class);
    }

    /*
        For now let's just keep all the machinery here.
     */

    private synchronized void scheduleCompilation() {
        // For now no scheduling, just compile and set synchronously.
        if (state == State.PROFILING) {
            state = State.COMPILING;
            callSite.setTarget(simpleInterpreterInvoker());
            forceCompile();
        }
    }

    void forceCompile() {
        var result = Compiler.compile(this);
        applyCompilationResult(result);
    }

    private synchronized void applyCompilationResult(Compiler.BatchResult batchResult) {
        var implClass = GeneratedCode.defineClass(batchResult);
        for (var entry : batchResult.results().entrySet()) {
            entry.getKey().updateCompiledForm(implClass, entry.getValue());
        }
    }

    private void updateCompiledForm(Class<?> generatedClass, Compiler.FunctionCompilationResult result) {
        MethodHandle genericMethod;
        MethodHandle specializedMethod = null;
        try {
            genericMethod = MethodHandles.lookup()
                .findStatic(generatedClass, result.genericMethodName(), MethodType.genericMethodType(implementationArity()));
            if (result.specializedMethodName() != null) {
                specializedMethod = MethodHandles.lookup()
                    .findStatic(generatedClass, result.specializedMethodName(), result.specializedMethodType());
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
        state = State.COMPILED;
        if (specializedMethod == null) {
            callSite.setTarget(dropFunctionArgument(genericMethod));
            // FIXME properly handle specializationCallSite if present
        } else {
            callSite.setTarget(
                dropFunctionArgument(
                    makeSpecializationGuard(genericMethod, specializedMethod, result.specializedMethodType())));
            if (specializationCallSite == null) {
                specializationCallSite = new VolatileCallSite(specializedMethod);
            } else {
                specializationCallSite.setTarget(specializedMethod);
            }
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
     * Take a method handle of a type involving some non-Object types and wrap
     * it so that it accepts and returns all Objects.
     */
    private MethodHandle generify(MethodHandle specialization) {
        MethodType genericType = MethodType.genericMethodType(specialization.type().parameterCount());
        MethodHandle generic = specialization.asType(genericType);
        return MethodHandles.catchException(generic, SquarePegException.class, EXTRACT_SQUARE_PEG);
    }

    @SuppressWarnings("unused") // called by generated code
    public static boolean checkSpecializationApplicability(MethodType specialization, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            Class<?> type = specialization.parameterType(i);
            Object arg = args[i];
            if (type.equals(int.class) && !(arg instanceof Integer)) return false;
            if (type.equals(boolean.class) && !(arg instanceof Boolean)) return false;
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
