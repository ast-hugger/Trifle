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
 * An object holding together all executable representations of a source function
 * (though not necessarily all of them are available at any given time): a tree of {@link
 * EvaluatorNode}s, a list of recovery interpreter instructions, method handles to generic
 * and compiled methods.
 *
 * <p>This is <em>not</em> a function value of the implemented language. For that, see
 * {@link Closure}.
 */
public class FunctionImplementation {

    /** The number of times a function is profiled before it's queued for compilation. */
    private static final long PROFILING_TARGET = Long.MAX_VALUE;

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
    private List<VariableDefinition> parameters;
    /**
     * All copied variables created during closure conversion.
     */
    private List<CopiedVariable> syntheticParameters;
    /**
     * The actual parameter list, beginning with synthetic parameters for copied down
     * values followed by apparent parameters.
     */
    private AbstractVariable[] allParameters;
    /*internal*/ FunctionProfile profile;
    private final int arity;
    private int id = -1;
    /*internal*/ int depth = -1;
    /** Shared by all {@code invokedynamics} linked to this function. */
    private final VolatileCallSite callSite;
    private final MethodHandle callSiteInvoker;
    private EvaluatorNode body;
    private int localsCount = -1;
    @Nullable private MethodType specializationType;
    @Nullable private VolatileCallSite specializationCallSite;
    /*internal*/ ACodeInstruction[] acode;
    private State state;

    FunctionImplementation(@NotNull Lambda definition) {
        this.definition = definition;
        this.arity = definition.arguments().size();
        this.state = State.INVALID;
//        this.callSite = new VolatileCallSite(profilingInterpreterInvoker());
        this.callSite = new VolatileCallSite(simpleInterpreterInvoker());
//        this.callSite = new VolatileCallSite(acodeInterpreterInvoker());
        this.callSiteInvoker = callSite.dynamicInvoker();
    }

    /** RESTRICTED. Intended for {@link FunctionTranslator}. */
    void partiallyInitialize(@NotNull List<VariableDefinition> parameters, @NotNull EvaluatorNode body) {
        this.parameters = parameters;
        this.profile = new FunctionProfile(parameters);
        this.body = body;
    }

    /** RESTRICTED. Intended for {@link FunctionAnalyzer.VariableIndexer}. */
    void finishInitialization(int localsCount) {
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

    public List<VariableDefinition> parameters() {
        return parameters;
    }

    public List<CopiedVariable> syntheticParameters() {
        return syntheticParameters;
    }

    public AbstractVariable[] allParameters() {
        return allParameters;
    }

    /**
     * RESTRICTED. Intended for {@link FunctionAnalyzer.VariableIndexer}.
     * Accept the synthetic variables into which free variable references have been
     * rewritten in this function.
     */
    /*internal*/ void setSyntheticParameters(Collection<CopiedVariable> variables) {
        this.syntheticParameters = new ArrayList<>(variables);
        this.allParameters = Stream.concat(syntheticParameters.stream(), parameters.stream())
            .toArray(AbstractVariable[]::new);
    }

    public EvaluatorNode body() {
        return body;
    }

    public int arity() {
        return arity;
    }

    public int frameSize() {
        return localsCount;
    }

    public ACodeInstruction[] acode() {
        return acode;
    }

    Object execute(Closure closure) {
        try {
            return callSiteInvoker.invokeExact(closure);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    Object execute(Closure function, Object arg) {
        try {
            return callSiteInvoker.invokeExact(function, arg);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    Object execute(Closure closure, Object arg1, Object arg2) {
        try {
            return callSiteInvoker.invokeExact(closure, arg1, arg2);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    /**
     * Return a call site invoking which executes the function in the best
     * currently possible mode. Invokedynamics calling this function all share
     * this call site. The type of the call site matches the arity of the
     * function, so invokedynamics with an incompatible signature will fail at
     * the bootstrap time unless special measures are taken by the bootstrapper.
     */
    public CallSite callSite(MethodType requestedType) {
        // At the moment everything is generically typed so no adaptation is necessary.
        if (requestedType.equals(specializationType)) {
            return specializationCallSite;
        } else if (requestedType.equals(callSite.type())){
            return callSite;
        } else {
            throw new UnsupportedOperationException("partial specialization is not yet supported");
        }
    }

    private MethodHandle profilingInterpreterInvoker() {
        var type = MethodType.genericMethodType(arity());
        type = type.insertParameterTypes(0, Closure.class);
        try {
            var handle = MethodHandles.lookup().findVirtual(FunctionImplementation.class, "profile", type);
            return handle.bindTo(this);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private MethodHandle simpleInterpreterInvoker() {
        var type = MethodType.genericMethodType(arity());
        type = type.insertParameterTypes(0, Closure.class);
        try {
            MethodHandle interpret = MethodHandles.lookup().findVirtual(Interpreter.class, "interpret", type);
            return MethodHandles.insertArguments(interpret, 0, Interpreter.INSTANCE);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private MethodHandle acodeInterpreterInvoker() {
        var type = MethodType.genericMethodType(arity());
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

    /*internal*/ synchronized void updateCompiledForm(
        MethodHandle genericMethod,
        @Nullable MethodType specializationType,
        @Nullable MethodHandle specializedMethod)
    {
        state = State.COMPILED;
        this.specializationType = specializationType;
        if (specializedMethod == null) {
            callSite.setTarget(dropFunctionArgument(genericMethod));
            // FIXME properly handle specializationCallSite if present
        } else {
            callSite.setTarget(
                dropFunctionArgument(
                    makeSpecializationGuard(genericMethod, specializedMethod, specializationType)));
            if (specializationCallSite == null) {
                specializationCallSite = new VolatileCallSite(specializedMethod);
            } else {
                specializationCallSite.setTarget(specializedMethod);
            }
        }
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
        Compiler.Result result = Compiler.compile(this);
        try {
            Class<?> implClass = GeneratedCode.defineClass(result);
            MethodHandle genericMethod = MethodHandles.lookup()
                .findStatic(implClass, Compiler.GENERIC_METHOD_NAME, MethodType.genericMethodType(arity()));
            MethodType specializedType = result.specializationType();
            MethodHandle specializedMethod = null;
            if (specializedType != null) {
                specializedMethod = MethodHandles.lookup()
                    .findStatic(implClass, Compiler.SPECIALIZED_METHOD_NAME, specializedType);
            }
            updateCompiledForm(genericMethod, specializedType, specializedMethod);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
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
        return definition.toString();
    }
}
