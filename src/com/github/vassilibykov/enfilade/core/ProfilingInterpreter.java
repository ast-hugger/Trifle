// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class ProfilingInterpreter extends Interpreter {
    public static final ProfilingInterpreter INSTANCE = new ProfilingInterpreter();

    static class ProfilingEvaluator extends Evaluator {
        ProfilingEvaluator(Object[] frame) {
            super(frame);
        }

        @Override
        public Object visitCall0(CallNode.Call0 call) {
            Object result = call.function().invoke();
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitCall1(CallNode.Call1 call) {
            Object arg = call.arg().accept(this);
            Object result = call.function().invoke(arg);
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitCall2(CallNode.Call2 call) {
            Object arg1 = call.arg1().accept(this);
            Object arg2 = call.arg2().accept(this);
            Object result = call.function().invoke(arg1, arg2);
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitIf(IfNode anIf) {
            Object testValue = anIf.condition().accept(this);
            if ((Boolean) testValue) {
                Object result = anIf.trueBranch().accept(this);
                // The count must be incremented after the branch. Counts logically track the cases
                // when a value has been produced by a branch, not when it has been invoked.
                // Descending into a branch may fail to produce a value if there is a return in the branch.
                anIf.trueBranchCount.incrementAndGet();
                return result;
            } else {
                Object result = anIf.falseBranch().accept(this);
                anIf.falseBranchCount.incrementAndGet();
                return result;
            }
        }

        @Override
        public Object visitLet(LetNode let) {
            Object value = let.initializer().accept(this);
            VariableDefinition variable = let.variable();
            frame[variable.genericIndex()] = value;
            variable.profile.recordValue(value);
            return let.body().accept(this);
        }

        @Override
        public Object visitVarSet(SetVariableNode set) {
            Object value = set.value().accept(this);
            VariableDefinition variable = set.variable();
            frame[variable.genericIndex()] = value;
            variable.profile.recordValue(value);
            return value;
        }
    }

    /*
        Instance
     */

    @Override
    public Object interpret(RuntimeFunction function) {
        Object[] frame = new Object[function.localsCount()];
        function.profile.recordInvocation(frame);
        try {
            Object result = function.body().accept(new ProfilingEvaluator(frame));
            function.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(RuntimeFunction function, Object arg) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg;
        function.profile.recordInvocation(frame);
        try {
            Object result = function.body().accept(new ProfilingEvaluator(frame));
            function.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(RuntimeFunction function, Object arg1, Object arg2) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg1;
        frame[1] = arg2;
        function.profile.recordInvocation(frame);
        try {
            Object result = function.body().accept(new ProfilingEvaluator(frame));
            function.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpretWithArgs(RuntimeFunction function, Object[] actualArguments) {
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
