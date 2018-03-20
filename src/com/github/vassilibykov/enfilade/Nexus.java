package com.github.vassilibykov.enfilade;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VolatileCallSite;

/**
 * A nexus of function representation and execution. Manages function
 * compilation, keeps track of any existing compiled form(s) of the function and
 * able to execute the function in either interpreted or compiled mode.
 */
class Nexus {
    /** The number of times a function is profiled before it's queued for compilation. */
    private static final int PROFILING_TARGET = 100;

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

    private final Function function;
    private final VolatileCallSite callSite;
    private final MethodHandle callSiteInvoker;
    private State state;

    Nexus(Function function) {
        this.function = function;
        this.state = State.PROFILING;
        this.callSite = new VolatileCallSite(profilingInterpreterInvoker());
        this.callSiteInvoker = callSite.dynamicInvoker();
    }

    /**
     * Return a call site invoking which executes the function in the best
     * currently possible mode. Invokedynamics calling this function all share
     * this call site. The type of the call site matches the arity of the
     * function, so invokedynamics with an incompatible signature will fail at
     * the bootstrap time unless special measures are taken by the bootstrapper.
     */
    public CallSite callSite(MethodType requiredType) {
        // At the moment everything is generically typed so no adaptation is necessary.
        return callSite;
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
        type = type.insertParameterTypes(0, Function.class);
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

    /*internal*/ synchronized void setCompiledForm(MethodHandle newCompiledForm) {
        this.state = State.COMPILED;
        this.callSite.setTarget(newCompiledForm);
    }

    /*
        For now let's just keep all the machinery here.
     */

    private static final GeneratedClassLoader classLoader = new GeneratedClassLoader(Nexus.class.getClassLoader());

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
        classLoader.add(result);
        try {
            Class<?> implClass = classLoader.loadClass(result.className());
            MethodHandle compiledMethod = MethodHandles.lookup()
                .findStatic(implClass, Compiler.IMPL_METHOD_NAME, MethodType.genericMethodType(function.arity()));
            setCompiledForm(compiledMethod);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
