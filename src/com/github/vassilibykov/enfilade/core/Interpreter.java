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
        protected Object[][] outerFrames;

        public Evaluator(Object[] frame, Object[][] outerFrames) {
            this.frame = frame;
            this.outerFrames = outerFrames;
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
            var frames = new Object[outerFrames.length + 1][];
            frames[0] = frame;
            System.arraycopy(outerFrames, 0, frames, 1, outerFrames.length);
            return new Closure(closure.function(), frames);
        }

        @Override
        public Object visitConst(ConstNode aConst) {
            return aConst.value();
        }

        @Override
        public Object visitFreeVarReference(FreeVariableReferenceNode varRef) {
            return outerFrames[varRef.frameIndex()][varRef.variable.genericIndex()];
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
        public Object visitSetFreeVar(SetFreeVariableNode set) {
            var value = set.value().accept(this);
            outerFrames[set.frameIndex()][set.variable.genericIndex] = value;
            return value;
        }

        @Override
        public Object visitSetVar(SetVariableNode set) {
            var value = set.value().accept(this);
            frame[set.variable.genericIndex] = value;
            return value;
        }

        @Override
        public Object visitVarReference(VariableReferenceNode varRef) {
            return frame[varRef.variable.genericIndex()];
        }
    }

    /*
        Instance
     */

    public Object interpret(Closure closure) {
        var implementation = closure.implementation;
        var frame = new Object[implementation.frameSize()];
        try {
            return implementation.body().accept(new Evaluator(frame, closure.outerFrames));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Closure closure, Object arg) {
        var implementation = closure.implementation;
        var frame = new Object[implementation.frameSize()];
        frame[0] = arg;
        try {
            return implementation.body().accept(new Evaluator(frame, closure.outerFrames));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Closure closure, Object arg1, Object arg2) {
        var implementation = closure.implementation;
        var frame = new Object[implementation.frameSize()];
        frame[0] = arg1;
        frame[1] = arg2;
        try {
            return implementation.body().accept(new Evaluator(frame, closure.outerFrames));
        } catch (ReturnException e) {
            return e.value;
        }
    }

//    public Object interpretWithArgs(Function  invocable, Object[] actualArguments) {
//        var frame = new Object[invocable.localsCount()];
//        System.arraycopy(actualArguments, 0, frame, 0, actualArguments.length);
//        try {
//            return invocable.body().accept(new Evaluator(frame, invocable.outerFrames()));
//        } catch (ReturnException e) {
//            return e.value;
//        }
//    }
}
