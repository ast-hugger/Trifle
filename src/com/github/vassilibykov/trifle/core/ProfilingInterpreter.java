// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

public class ProfilingInterpreter extends Interpreter {
    public static final ProfilingInterpreter INSTANCE = new ProfilingInterpreter();

    static class ProfilingEvaluator extends Evaluator {
        ProfilingEvaluator(Object[] frame) {
            super(frame);
        }

        @Override
        public Object visitCall(CallNode call) {
            var result = call.dispatcher().execute(call, this);
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitIf(IfNode anIf) {
            Object testValue = anIf.condition().accept(this);
            boolean test;
            try {
                test = (boolean) testValue;
            } catch (ClassCastException e) {
                throw RuntimeError.booleanExpected(testValue);
            }
            if (test) {
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
            value = let.initializer().accept(this);
            variable.initValueIn(frame, value);
            variable.profile.recordValue(value);
            return let.body().accept(this);
        }

        @Override
        public Object visitSetVar(SetVariableNode setVar) {
            var value = super.visitSetVar(setVar);
            setVar.variable().profile().recordValue(value);
            return value;
        }

        @Override
        public Object visitWhile(WhileNode whileNode) {
            Object result = null;
            while (evaluateCondition(whileNode.condition())) {
                result = whileNode.body().accept(this);
                whileNode.bodyCount.incrementAndGet();
            }
            return result;
        }
    }

    /*
        Instance
     */

    @Override
    public Object interpret(FunctionImplementation function, Object[] args) {
        var frame = new Object[function.frameSize()];
        var allParameters = function.allParameters();
        for (int i = 0; i < args.length; i++) {
            allParameters[i].setupArgumentIn(frame, args[i]);
        }
        function.profile.recordArguments(frame);
        Object result;
        try {
            result = function.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            result = e.value;
        }
        function.profile.recordResult(result);
        return result;
    }
}
