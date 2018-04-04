// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class Interpreter {
    public static final Interpreter INSTANCE = new Interpreter();

    static class ReturnException extends RuntimeException {
        final Object value;

        ReturnException(Object value) {
            this.value = value;
        }
    }

    public static class Evaluator implements EvaluatorNode.Visitor<Object> {
        protected final Object[] frame;

        public Evaluator(Object[] frame) {
            this.frame = frame;
        }

        @Override
        public Object visitBlock(BlockNode block) {
            EvaluatorNode[] expressions = block.expressions();
            int bodySize = expressions.length - 1;
            int i;
            for (i = 0; i < bodySize; i++) expressions[i].accept(this);
            return expressions[i].accept(this);
        }

        @Override
        public Object visitCall0(CallNode.Call0 call) {
            var function = call.function().accept(this);
            return ((Closure) function).invoke();
        }

        @Override
        public Object visitCall1(CallNode.Call1 call) {
            var function = call.function().accept(this);
            var arg = call.arg().accept(this);
            return ((Closure) function).invoke(arg);
        }

        @Override
        public Object visitCall2(CallNode.Call2 call) {
            var function = call.function().accept(this);
            var arg1 = call.arg1().accept(this);
            var arg2 = call.arg2().accept(this);
            return ((Closure) function).invoke(arg1, arg2);
        }

        @Override
        public Object visitClosure(ClosureNode closure) {
            int[] indicesToCopy = closure.copiedVariablesGenericIndices;
            var size = indicesToCopy.length;
            var copies = new Object[size];
            for (int i = 0; i < size; i++) copies[i] = frame[indicesToCopy[i]];
            return new Closure(closure.function(), copies);
        }

        @Override
        public Object visitConst(ConstNode aConst) {
            return aConst.value();
        }

        @Override
        public Object visitGetVar(GetVariableNode varRef) {
            return varRef.variable().getValueIn(frame);
        }

        @Override
        public Object visitIf(IfNode anIf) {
            Object testValue = anIf.condition().accept(this);
            if ((Boolean) testValue) {
                return anIf.trueBranch().accept(this);
            } else {
                return anIf.falseBranch().accept(this);
            }
        }

        @Override
        public Object visitLet(LetNode let) {
            var var = let.variable();
            var value = let.initializer().accept(this);
            var.initValueIn(frame, value);
            return let.body().accept(this);
        }

        @Override
        public Object visitLetrec(LetrecNode letrec) {
            var var = letrec.variable();
            var.initValueIn(frame, null);
            var value = letrec.initializer().accept(this);
            var.setValueIn(frame, value);
            return letrec.body().accept(this);
        }

        @Override
        public Object visitPrimitive1(Primitive1Node primitive) {
            return primitive.apply(primitive.argument().accept(this));
        }

        @Override
        public Object visitPrimitive2(Primitive2Node primitive) {
            return primitive.apply(
                primitive.argument1().accept(this),
                primitive.argument2().accept(this));
        }

        @Override
        public Object visitReturn(ReturnNode ret) {
            throw new ReturnException(ret.value().accept(this));
        }

        @Override
        public Object visitSetVar(SetVariableNode set) {
            var value = set.value().accept(this);
            set.variable().setValueIn(frame, value);
            return value;
        }

        @Override
        public Object visitConstantFunction(ConstantFunctionNode constFunction) {
            return constFunction.closure();
        }
    }

    /*
        Instance
     */

    public Object interpret(FunctionImplementation implFunction) {
        var frame = new Object[implFunction.frameSize()];
        try {
            return implFunction.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(FunctionImplementation implFunction, Object arg) {
        var frame = new Object[implFunction.frameSize()];
        implFunction.allParameters()[0].setupArgumentIn(frame, arg);
        try {
            return implFunction.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(FunctionImplementation implFunction, Object arg1, Object arg2) {
        var frame = new Object[implFunction.frameSize()];
        var allParameters = implFunction.allParameters();
        allParameters[0].setupArgumentIn(frame, arg1);
        allParameters[1].setupArgumentIn(frame, arg2);
        try {
            return implFunction.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(FunctionImplementation implFunction, Object arg1, Object arg2, Object arg3) {
        var frame = new Object[implFunction.frameSize()];
        var allParameters = implFunction.allParameters();
        allParameters[0].setupArgumentIn(frame, arg1);
        allParameters[1].setupArgumentIn(frame, arg2);
        allParameters[2].setupArgumentIn(frame, arg3);
        try {
            return implFunction.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
