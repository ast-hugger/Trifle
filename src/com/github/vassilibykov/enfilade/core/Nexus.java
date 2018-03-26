// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VolatileCallSite;

/**
 * A nexus of function representation and execution. Manages function
 * compilation, keeps track of any existing compiled form(s) of the function,
 * and is able to execute the function in either interpreted or compiled mode.
 */
class Nexus {
    /** The number of times a function is profiled before it's queued for compilation. */
    private static final long PROFILING_TARGET = Long.MAX_VALUE;

    private enum State {
        /**
         * The nexus is currently collecting type information for its function.
         * The {@link #callSite} is pointing at one of the {@link #interpret}
         * methods.
         */
        PROFILING,
        /**
         * The nexus has finished collecting type information. The function is
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

    private final RunnableFunction function;
    private final VolatileCallSite callSite;
    private final MethodHandle callSiteInvoker;
    @Nullable private MethodType specializationType;
    @Nullable private VolatileCallSite specializationCallSite;
    private State state;

    Nexus(RunnableFunction function) {
        this.function = function;
        this.state = State.PROFILING;
        this.callSite = new VolatileCallSite(profilingInterpreterInvoker());
//        this.callSite = new VolatileCallSite(simpleInterpreterInvoker());
        this.callSiteInvoker = callSite.dynamicInvoker();
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
        MethodType type = MethodType.genericMethodType(function.arity());
        try {
            MethodHandle interpret = MethodHandles.lookup().findVirtual(Nexus.class, "interpret", type);
            return interpret.bindTo(this);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private MethodHandle simpleInterpreterInvoker() {
        MethodType type = MethodType.genericMethodType(function.arity());
        type = type.insertParameterTypes(0, RunnableFunction.class);
        try {
            MethodHandle interpret = MethodHandles.lookup().findVirtual(Interpreter.class, "interpret", type);
            return MethodHandles.insertArguments(interpret, 0, Interpreter.INSTANCE, function);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public Object interpret() {
        Object result = ProfilingInterpreter.INSTANCE.interpret(function);
        if (function.profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object interpret(Object arg) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(function, arg);
        if (function.profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object interpret(Object arg1, Object arg2) {
        Object result = ProfilingInterpreter.INSTANCE.interpret(function, arg1, arg2);
        if (function.profile.invocationCount() > PROFILING_TARGET) {
            scheduleCompilation();
        }
        return result;
    }

    public Object invoke() {
        try {
            return callSiteInvoker.invokeExact();
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    public Object invoke(Object arg) {
        try {
            return callSiteInvoker.invokeExact(arg);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    public Object invoke(Object arg1, Object arg2) {
        try {
            return callSiteInvoker.invokeExact(arg1, arg2);
        } catch (Throwable throwable) {
            throw new InvocationException(throwable);
        }
    }

    /*internal*/ synchronized void updateCompiledForm(
        MethodHandle genericMethod,
        @Nullable MethodType specializationType,
        @Nullable MethodHandle specializedMethod)
    {
        state = State.COMPILED;
        this.specializationType = specializationType;
        if (specializedMethod == null) {
            callSite.setTarget(genericMethod);
            // FIXME properly handle specializationCallSite if present
        } else {
            callSite.setTarget(makeSpecializationGuard(genericMethod, specializedMethod, specializationType));
            if (specializationCallSite == null) {
                specializationCallSite = new VolatileCallSite(specializedMethod);
            } else {
                specializationCallSite.setTarget(specializedMethod);
            }
        }
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
        Compiler.Result result = Compiler.compile(function);
        try {
            Class<?> implClass = GeneratedCode.defineClass(result);
            MethodHandle genericMethod = MethodHandles.lookup()
                .findStatic(implClass, Compiler.GENERIC_METHOD_NAME, MethodType.genericMethodType(function.arity()));
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

    public static boolean checkSpecializationApplicability(MethodType specialization, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            Class<?> type = specialization.parameterType(i);
            Object arg = args[i];
            if (type.equals(int.class) && !(arg instanceof Integer)) return false;
            if (type.equals(boolean.class) && !(arg instanceof Boolean)) return false;
        }
        return true;
    }

    public static Object extractSquarePeg(SquarePegException exception) {
        return exception.value;
    }

    private static final MethodHandle CHECK;
    private static final MethodHandle EXTRACT_SQUARE_PEG;

    static {
        try {
            CHECK = MethodHandles.lookup().findStatic(
                Nexus.class,
                "checkSpecializationApplicability",
                MethodType.methodType(boolean.class, MethodType.class, Object[].class));
            EXTRACT_SQUARE_PEG = MethodHandles.lookup().findStatic(
                Nexus.class,
                "extractSquarePeg", MethodType.methodType(Object.class, SquarePegException.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
