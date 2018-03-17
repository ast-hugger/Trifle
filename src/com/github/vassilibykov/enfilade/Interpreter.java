package com.github.vassilibykov.enfilade;

public class Interpreter {

    private static class Frame {
        private final Frame parentFrame;
        private final Object[] locals;

        Frame(Frame parentFrame, int localsCount) {
            this.parentFrame = parentFrame;
            this.locals = new Object[localsCount];
        }
        /*
         Interestingly, replacing Frame with just an Object[] and saving the caller frame in a local
         in the call method reduces runtime by about 15%. However, dispensing with object allocation
         altogether by creating a single 'stack' Object[] with a current frame base index makes no
         difference compared to using Object[] instead of Frame.
         */
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
            currentFrame = newFrame(currentFrame, call.method());
            Object result = runMethod(call.method());
            currentFrame = currentFrame.parentFrame;
            return result;
        }

        @Override
        public Object visitCall1(Call1 call) {
            Frame calleeFrame = newFrame(currentFrame, call.method());
            calleeFrame.locals[0] = call.arg().accept(this);
            currentFrame = calleeFrame;
            Object result = runMethod(call.method());
            currentFrame = currentFrame.parentFrame;
            return result;
        }

        @Override
        public Object visitCall2(Call2 call) {
            Frame calleeFrame = newFrame(currentFrame, call.method());
            calleeFrame.locals[0] = call.arg1().accept(this);
            calleeFrame.locals[1] = call.arg2().accept(this);
            currentFrame = calleeFrame;
            Object result = runMethod(call.method());
            currentFrame = currentFrame.parentFrame;
            return result;
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
    private Frame currentFrame;

    public Object interpret(Method method, Object[] actualArguments) {
        currentFrame = newFrame(null, method);
        System.arraycopy(actualArguments, 0, currentFrame.locals, 0, actualArguments.length);
        return runMethod(method);
    }

    private Frame newFrame(Frame parentFrame, Method method) {
        return new Frame(parentFrame, method.localsCount());
    }

    private Object runMethod(Method method) {
        try {
            return method.body().accept(evaluator);
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
