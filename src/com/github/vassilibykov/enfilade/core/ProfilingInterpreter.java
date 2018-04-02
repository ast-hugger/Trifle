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
            var function = (Closure) call.function().accept(this);
            var result = function.invoke();
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitCall1(CallNode.Call1 call) {
            var function = (Closure) call.function().accept(this);
            var arg = call.arg().accept(this);
            var result = function.invoke(arg);
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitCall2(CallNode.Call2 call) {
            var function = (Closure) call.function().accept(this);
            var arg1 = call.arg1().accept(this);
            var arg2 = call.arg2().accept(this);
            var result = function.invoke(arg1, arg2);
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
            VariableDefinition variable = let.variable();
            Object value;
            if (let.isLetrec()) {
                variable.initValueIn(frame, null);
                value = let.initializer().accept(this);
                variable.setValueIn(frame, value);
            } else {
                value = let.initializer().accept(this);
                variable.initValueIn(frame, value);
            }
            variable.profile.recordValue(value);
            return let.body().accept(this);
        }

        @Override
        public Object visitSetVar(SetVariableNode setVar) {
            var value = super.visitSetVar(setVar);
            setVar.variable().profile().recordValue(value);
            return value;
        }
    }

    /*
        Instance
     */

    @Override
    public Object interpret(FunctionImplementation implementation) {
        var frame = new Object[implementation.frameSize()];
        implementation.profile.recordInvocation(frame);
        try {
            Object result = implementation.body().accept(new ProfilingEvaluator(frame));
            implementation.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(FunctionImplementation implFunction, Object arg) {
        var frame = new Object[implFunction.frameSize()];
        implFunction.allParameters()[0].setupArgumentIn(frame, arg);
        implFunction.profile.recordInvocation(frame);
        try {
            Object result = implFunction.body().accept(new ProfilingEvaluator(frame));
            implFunction.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(FunctionImplementation implFunction, Object arg1, Object arg2) {
        var frame = new Object[implFunction.frameSize()];
        implFunction.allParameters()[0].setupArgumentIn(frame, arg1);
        implFunction.allParameters()[1].setupArgumentIn(frame, arg2);
        implFunction.profile.recordInvocation(frame);
        try {
            Object result = implFunction.body().accept(new ProfilingEvaluator(frame));
            implFunction.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(FunctionImplementation implFunction, Object arg1, Object arg2, Object arg3) {
        var frame = new Object[implFunction.frameSize()];
        implFunction.allParameters()[0].setupArgumentIn(frame, arg1);
        implFunction.allParameters()[1].setupArgumentIn(frame, arg2);
        implFunction.allParameters()[2].setupArgumentIn(frame, arg3);
        implFunction.profile.recordInvocation(frame);
        try {
            Object result = implFunction.body().accept(new ProfilingEvaluator(frame));
            implFunction.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
