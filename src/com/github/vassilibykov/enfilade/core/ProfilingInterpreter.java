// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class ProfilingInterpreter extends Interpreter {
    public static final ProfilingInterpreter INSTANCE = new ProfilingInterpreter();

    static class ProfilingEvaluator extends Evaluator {
        ProfilingEvaluator(Object[] frame, Object[][] outerFrames) {
            super(frame, outerFrames);
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
            Object value = let.initializer().accept(this);
            VariableDefinition variable = let.variable();
            frame[variable.genericIndex()] = value;
            variable.profile.recordValue(value);
            return let.body().accept(this);
        }

        @Override
        public Object visitSetFreeVar(SetFreeVariableNode setNode) {
            var value = super.visitSetFreeVar(setNode);
            setNode.variable.profile.recordValue(value);
            return value;
        }

        @Override
        public Object visitSetVar(SetVariableNode setVar) {
            var value = super.visitSetVar(setVar);
            setVar.variable.profile.recordValue(value);
            return value;
        }
    }

    /*
        Instance
     */

    @Override
    public Object interpret(Closure closure) {
        var implementation = closure.implementation;
        var frame = new Object[implementation.frameSize()];
        implementation.profile.recordInvocation(frame);
        try {
            Object result = implementation.body().accept(new ProfilingEvaluator(frame, closure.outerFrames));
            implementation.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(Closure closure, Object arg) {
        var implementation = closure.implementation;
        var frame = new Object[implementation.frameSize()];
        frame[0] = arg;
        implementation.profile.recordInvocation(frame);
        try {
            Object result = implementation.body().accept(new ProfilingEvaluator(frame, closure.outerFrames));
            implementation.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

    @Override
    public Object interpret(Closure closure, Object arg1, Object arg2) {
        var implementation = closure.implementation;
        var frame = new Object[implementation.frameSize()];
        frame[0] = arg1;
        frame[1] = arg2;
        implementation.profile.recordInvocation(frame);
        try {
            Object result = implementation.body().accept(new ProfilingEvaluator(frame, closure.outerFrames));
            implementation.profile.recordResult(result);
            return result;
        } catch (ReturnException e) {
            return e.value;
        }
    }

//    @Override
//    public Object interpretWithArgs(Invocable function, Object[] actualArguments) {
//        Object[] frame = new Object[function.frameSize()];
//        System.arraycopy(actualArguments, 0, frame, 0, actualArguments.length);
//        function.profile().recordInvocation(frame);
//        try {
//            return function.body().accept(new ProfilingEvaluator(frame));
//        } catch (ReturnException e) {
//            return e.value;
//        }
//    }
}
