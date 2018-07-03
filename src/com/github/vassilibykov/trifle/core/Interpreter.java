// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

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
            int exprCount = expressions.length;
            if (exprCount > 0) {
                int i;
                for (i = 0; i < exprCount - 1; i++) expressions[i].accept(this);
                return expressions[i].accept(this);
            } else {
                return null;
            }
        }

        @Override
        public Object visitCall(CallNode call) {
            return call.dispatcher().execute(call, this);
        }

        @Override
        public Object visitClosure(ClosureNode closure) {
            int[] indicesToCopy = closure.copiedVariableIndices;
            var size = indicesToCopy.length;
            var copies = new Object[size];
            for (int i = 0; i < size; i++) copies[i] = frame[indicesToCopy[i]];
            return Closure.create(closure.function(), copies);
        }

        @Override
        public Object visitConstant(ConstantNode aConst) {
            return aConst.value();
        }

        @Override
        public Object visitGetVar(GetVariableNode varRef) {
            return varRef.variable().getValueIn(frame);
        }

        @Override
        public Object visitFreeFunctionReference(FreeFunctionReferenceNode reference) {
            return reference.target();
        }

        @Override
        public Object visitIf(IfNode anIf) {
            if (evaluateCondition(anIf.condition())) {
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
        public Object visitPrimitive1(Primitive1Node primitiveNode) {
            return primitiveNode.implementation().apply(primitiveNode.argument().accept(this));
        }

        @Override
        public Object visitPrimitive2(Primitive2Node primitiveNode) {
            return primitiveNode.implementation().apply(
                primitiveNode.argument1().accept(this),
                primitiveNode.argument2().accept(this));
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
        public Object visitWhile(WhileNode whileNode) {
            Object result = null;
            while (evaluateCondition(whileNode.condition())) {
                result = whileNode.body().accept(this);
            }
            return result;
        }

        protected boolean evaluateCondition(EvaluatorNode condition) {
            var value = condition.accept(this);
            try {
                return (Boolean) value;
            } catch (ClassCastException e) {
                throw RuntimeError.booleanExpected(value);
            }
        }
    }

    /*
        Instance
     */

    public Object interpret(FunctionImplementation function, Object[] args) {
        var frame = new Object[function.frameSize()];
        for (int i = 0; i < args.length; i++) {
            function.allParameters()[i].setupArgumentIn(frame, args[i]);
        }
        try {
            return function.body().accept(new ProfilingInterpreter.ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
