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
        public Object visitCall0(CallNode.Call0 call) {
            return call.function().invoke();
        }

        @Override
        public Object visitCall1(CallNode.Call1 call) {
            Object arg = call.arg().accept(this);
            return call.function().invoke(arg);
        }

        @Override
        public Object visitCall2(CallNode.Call2 call) {
            Object arg1 = call.arg1().accept(this);
            Object arg2 = call.arg2().accept(this);
            return call.function().invoke(arg1, arg2);
        }

        @Override
        public Object visitConst(ConstNode aConst) {
            return aConst.value();
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
            Object value = let.initializer().accept(this);
            VariableDefinition variable = let.variable();
            frame[variable.genericIndex()] = value;
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
        public Object visitBlock(BlockNode block) {
            EvaluatorNode[] expressions = block.expressions();
            int bodySize = expressions.length - 1;
            int i;
            for (i = 0; i < bodySize; i++) expressions[i].accept(this);
            return expressions[i].accept(this);
        }

        @Override
        public Object visitRet(ReturnNode ret) {
            throw new ReturnException(ret.value().accept(this));
        }

        @Override
        public Object visitVarSet(SetVariableNode set) {
            Object value = set.value().accept(this);
            frame[set.variable.genericIndex()] = value;
            return value;
        }

        @Override
        public Object visitVarRef(VariableReferenceNode varRef) {
            return frame[varRef.variable.genericIndex()];
        }
    }

    /*
        Instance
     */

    public Object interpret(RuntimeFunction function) {
        Object[] frame = new Object[function.localsCount()];
        try {
            return function.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(RuntimeFunction function, Object arg) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg;
        try {
            return function.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(RuntimeFunction function, Object arg1, Object arg2) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg1;
        frame[1] = arg2;
        try {
            return function.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpretWithArgs(RuntimeFunction function, Object[] actualArguments) {
        Object[] frame = new Object[function.localsCount()];
        System.arraycopy(actualArguments, 0, frame, 0, actualArguments.length);
        try {
            return function.body().accept(new Evaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
