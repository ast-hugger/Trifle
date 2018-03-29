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
            int[] indicesToCopy = closure.indicesToCopy;
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
            if (let.isLetrec()) {
                var.initValueIn(frame, null);
                var value = let.initializer().accept(this);
                var.setValueIn(frame, value);
            } else {
                var value = let.initializer().accept(this);
                var.initValueIn(frame, value);
            }
            return let.body().accept(this);
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
        public Object visitRet(ReturnNode ret) {
            throw new ReturnException(ret.value().accept(this));
        }

        @Override
        public Object visitSetVar(SetVariableNode set) {
            var value = set.value().accept(this);
            set.variable().setValueIn(frame, value);
            return value;
        }

        @Override
        public Object visitTopLevelFunction(TopLevelFunctionNode topLevelFunction) {
            return topLevelFunction.binding.closure();
        }
    }

    /*
        Instance
     */

    public Object interpret(Closure closure) {
        var implFunction = closure.implementation;
        var frame = new Object[implFunction.frameSize()];
        var copiedCount = closure.copiedValues.length;
        System.arraycopy(closure.copiedValues, 0, frame, 0, copiedCount);
        try {
            return implFunction.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Closure closure, Object arg) {
        var implFunction = closure.implementation;
        var frame = new Object[implFunction.frameSize()];
        var copiedCount = closure.copiedValues.length;
        System.arraycopy(closure.copiedValues, 0, frame, 0, copiedCount);
        implFunction.parameters().get(0).initValueIn(frame, arg);
        try {
            return implFunction.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Closure closure, Object arg1, Object arg2) {
        var implFunction = closure.implementation;
        var frame = new Object[implFunction.frameSize()];
        var copiedCount = closure.copiedValues.length;
        System.arraycopy(closure.copiedValues, 0, frame, 0, copiedCount);
        implFunction.parameters().get(0).initValueIn(frame, arg1);
        implFunction.parameters().get(1).initValueIn(frame, arg2);
        try {
            return implFunction.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
