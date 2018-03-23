// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class ProfilingInterpreter extends Interpreter {
    public static final ProfilingInterpreter INSTANCE = new ProfilingInterpreter();

    static class ProfilingEvaluator extends Evaluator {
        ProfilingEvaluator(Object[] frame) {
            super(frame);
        }

        @Override
        public Object visitCall0(Call0 call) {
            Object result = call.function().nexus.invoke();
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitCall1(Call1 call) {
            Object arg = call.arg().accept(this);
            Object result = call.function().nexus.invoke(arg);
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitCall2(Call2 call) {
            Object arg1 = call.arg1().accept(this);
            Object arg2 = call.arg2().accept(this);
            Object result = call.function().nexus.invoke(arg1, arg2);
            call.profile.recordValue(result);
            return result;
        }

        @Override
        public Object visitConst(Const aConst) {
            aConst.evaluatedWhileProfiling = true;
            return super.visitConst(aConst);
        }

        @Override
        public Object visitLet(Let let) {
            Object value = let.initializer().accept(this);
            Variable variable = let.variable();
            frame[variable.index()] = value;
            variable.profile.recordValue(value);
            return let.body().accept(this);
        }

        @Override
        public Object visitPrimitive1(Primitive1 primitive) {
            primitive.evaluatedWhileProfiling = true;
            return super.visitPrimitive1(primitive);
        }

        @Override
        public Object visitPrimitive2(Primitive2 primitive) {
            primitive.evaluatedWhileProfiling = true;
            return super.visitPrimitive2(primitive);
        }

        @Override
        public Object visitVarRef(VarRef varRef) {
            varRef.evaluatedWhileProfiling = true;
            return super.visitVarRef(varRef);
        }

        @Override
        public Object visitVarSet(VarSet set) {
            Object value = set.value().accept(this);
            Variable variable = set.variable();
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
            Object result = function.body().accept(new ProfilingEvaluator(frame));
            function.profile.recordResult(result);
            return result;
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
            Object result = function.body().accept(new ProfilingEvaluator(frame));
            function.profile.recordResult(result);
            return result;
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
            Object result = function.body().accept(new ProfilingEvaluator(frame));
            function.profile.recordResult(result);
            return result;
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
