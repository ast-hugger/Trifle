package com.github.vassilibykov.enfilade.core;

public class ProfilingInterpreter extends Interpreter {
    public static final ProfilingInterpreter INSTANCE = new ProfilingInterpreter();

    static class ProfilingEvaluator extends Evaluator {
        ProfilingEvaluator(Object[] frame) {
            super(frame);
        }

        @Override
        public Object visitLet(Let let) {
            Object value = let.initializer().accept(this);
            Var variable = let.variable();
            frame[variable.index()] = value;
            variable.profile.recordValue(value);
            return let.body().accept(this);
        }

        @Override
        public Object visitSetVar(SetVar set) {
            Object value = set.value().accept(this);
            Var variable = set.variable();
            frame[variable.index()] = value;
            variable.profile.recordValue(value);
            return value;
        }
    }

    /*
        Instance
     */

    @Override
    public Object interpret(Function function) {
        Object[] frame = new Object[function.localsCount()];
        function.profile.recordInvocation(frame);
        try {
            return function.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(Function function, Object arg) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg;
        function.profile.recordInvocation(frame);
        try {
            return function.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(Function function, Object arg1, Object arg2) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg1;
        frame[1] = arg2;
        function.profile.recordInvocation(frame);
        try {
            return function.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpretWithArgs(Function function, Object[] actualArguments) {
        Object[] frame = new Object[function.localsCount()];
        System.arraycopy(actualArguments, 0, frame, 0, actualArguments.length);
        function.profile.recordInvocation(frame);
        try {
            return function.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
