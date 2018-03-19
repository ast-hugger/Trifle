package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.TestOnly;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;

/**
 * A nexus of function representation and execution. Keeps track of existing
 * compiled form(s) of the function and able to execute the function in
 * either interpreted or compiled mode.
 */
class Nexus {
    /** The number of times a function is executed interpreted before it's queued for compilation. */
    private static final int PROFILING_THRESHOLD = 100;

    /*
        Instance
     */

    private final Function function;
    volatile MethodHandle compiledForm;
    private SwitchPoint invalidator;

    Nexus(Function function) {
        this.function = function;
        this.invalidator = new SwitchPoint();
    }

    // TODO this and others shouldn't be wholesale synchronized; not around the interpret part anyway
    public Object invoke() {
        MethodHandle compiled = compiledForm;
        if (compiled != null) {
            try {
                return compiled.invoke();
            } catch (Throwable e) {
                throw new RuntimeException("error invoking function compiled form", e); // TODO should do something better
            }
        } else {
            Object result = Interpreter.INSTANCE.interpret(function);
            if (function.profile.invocationCount() > PROFILING_THRESHOLD) {
                scheduleCompilation();
            }
            return result;
        }
    }

    public synchronized Object invoke(Object arg) {
        if (compiledForm != null) {
            try {
                return compiledForm.invoke(arg);
            } catch (Throwable e) {
                throw new RuntimeException("error invoking function compiled form", e); // TODO should do something better
            }
        } else {
            Object result = Interpreter.INSTANCE.interpret(function, arg);
            if (function.profile.invocationCount() > PROFILING_THRESHOLD) {
                scheduleCompilation();
            }
            return result;
        }
    }

    public synchronized Object invoke(Object arg1, Object arg2) {
        if (compiledForm != null) {
            try {
                return compiledForm.invoke(arg1, arg2);
            } catch (Throwable e) {
                throw new RuntimeException("error invoking function compiled form", e); // TODO should do something better
            }
        } else {
            Object result = Interpreter.INSTANCE.interpret(function, arg1, arg2);
            if (function.profile.invocationCount() > PROFILING_THRESHOLD) {
                scheduleCompilation();
            }
            return result;
        }
    }

    /*internal*/ synchronized void setCompiledForm(MethodHandle newCompiledForm) {
        boolean replacingOld = this.compiledForm != null;
        this.compiledForm = newCompiledForm;
        if (replacingOld) {
            SwitchPoint.invalidateAll(new SwitchPoint[]{invalidator}); // TODO this is likely to be batched in future
            this.invalidator = new SwitchPoint();
        }
    }

    /*
        For now let's just keep all the machinery here.
     */

    private static final GeneratedClassLoader classLoader = new GeneratedClassLoader(Nexus.class.getClassLoader());

    private synchronized void scheduleCompilation() {
        // For now no scheduling, just compile and set synchronously.
        forceCompile();
    }

    @TestOnly
    void forceCompile() {
        if (compiledForm == null) {
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
}
