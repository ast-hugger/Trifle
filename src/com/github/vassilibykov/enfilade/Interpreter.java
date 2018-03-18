package com.github.vassilibykov.enfilade;

public class Interpreter {

    static class Frame {
        final Frame parentFrame;
        final Object[] locals;

        Frame(Frame parentFrame, int localsCount) {
            this.parentFrame = parentFrame;
            this.locals = new Object[localsCount];
        }
    }

    private static class ReturnException extends RuntimeException {
        private Object value;

        ReturnException(Object value) {
            this.value = value;
        }
    }

    private class Evaluator implements Expression.Visitor<Object> {
        @Override
        public Object visitCall0(Call0 call) {
            Method callee = call.method();
            Frame calleeFrame = newFrame(currentFrame, callee);
            callee.profile.recordInvocation(calleeFrame);
            return runMethod(callee, calleeFrame);
        }

        @Override
        public Object visitCall1(Call1 call) {
            Method callee = call.method();
            Frame calleeFrame = newFrame(currentFrame, callee);
            calleeFrame.locals[0] = call.arg().accept(this);
            callee.profile.recordInvocation(calleeFrame);
            return runMethod(callee, calleeFrame);
        }

        @Override
        public Object visitCall2(Call2 call) {
            Method callee = call.method();
            Frame calleeFrame = newFrame(currentFrame, callee);
            calleeFrame.locals[0] = call.arg1().accept(this);
            calleeFrame.locals[1] = call.arg2().accept(this);
            callee.profile.recordInvocation(calleeFrame);
            return runMethod(callee, calleeFrame);
        }

        @Override
        public Object visitConst(Const aConst) {
            return aConst.value();
        }

        @Override
        public Object visitIf(If anIf) {
            Object testValue = anIf.condition().accept(this);
            if ((Boolean) testValue) {
                return anIf.trueBranch().accept(this);
            } else {
                return anIf.falseBranch().accept(this);
            }
        }

        @Override
        public Object visitLet(Let let) {
            Object value = let.initializer().accept(this);
            currentFrame.locals[let.variable().index()] = value;
            currentMethod.profile.recordVarStore(let.variable(), value);
            return let.body().accept(this);
        }

        @Override
        public Object visitPrimitive1(Primitive1 primitive) {
            return primitive.apply(primitive.argument().accept(this));
        }

        @Override
        public Object visitPrimitive2(Primitive2 primitive) {
            return primitive.apply(
                primitive.argument1().accept(this),
                primitive.argument2().accept(this));
        }

        @Override
        public Object visitProg(Prog prog) {
            Expression[] expressions = prog.expressions();
            int bodySize = expressions.length - 1;
            int i;
            for (i = 0; i < bodySize; i++) expressions[i].accept(this);
            return expressions[i].accept(this);
        }

        @Override
        public Object visitRet(Ret ret) {
            throw new ReturnException(ret.value().accept(this));
        }

        @Override
        public Object visitSetVar(SetVar set) {
            Object value = set.value().accept(this);
            currentFrame.locals[set.variable().index()] = value;
            currentMethod.profile.recordVarStore(set.variable(), value);
            return value;
        }

        @Override
        public Object visitVar(Var var) {
            return currentFrame.locals[var.index()];
        }
    }

    /*
        Instance
     */

    private final Evaluator evaluator = new Evaluator();
    private Method currentMethod;
    private Frame currentFrame;

    public Object interpret(Method method, Object[] actualArguments) {
        currentFrame = null;
        Frame frame = newFrame(null, method);
        System.arraycopy(actualArguments, 0, frame.locals, 0, actualArguments.length);
        method.profile.recordInvocation(frame);
        return runMethod(method, frame);
    }

    private Frame newFrame(Frame parentFrame, Method method) {
        return new Frame(parentFrame, method.localsCount());
    }

    private Object runMethod(Method method, Frame frame) {
        Method oldMethod = currentMethod;
        Frame oldFrame = currentFrame;
        currentMethod = method;
        currentFrame = frame;
        try {
            return method.body().accept(evaluator);
        } catch (ReturnException e) {
            return e.value;
        } finally {
            currentMethod = oldMethod;
            currentFrame = oldFrame;
        }
    }
}
