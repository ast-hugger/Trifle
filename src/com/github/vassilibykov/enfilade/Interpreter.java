package com.github.vassilibykov.enfilade;

public class Interpreter {

    public static final Interpreter INSTANCE = new Interpreter();

    private static class ReturnException extends RuntimeException {
        private Object value;

        ReturnException(Object value) {
            this.value = value;
        }
    }

    private static class ProfilingEvaluator implements Expression.Visitor<Object> {
        final Object[] frame;

        private ProfilingEvaluator(Object[] frame) {
            this.frame = frame;
        }

        @Override
        public Object visitCall0(Call0 call) {
            return call.method().nexus.invoke();
        }

        @Override
        public Object visitCall1(Call1 call) {
            Object arg = call.arg().accept(this);
            return call.method().nexus.invoke(arg);
        }

        @Override
        public Object visitCall2(Call2 call) {
            Object arg1 = call.arg1().accept(this);
            Object arg2 = call.arg2().accept(this);
            return call.method().nexus.invoke(arg1, arg2);
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
            Var variable = let.variable();
            frame[variable.index()] = value;
            variable.profile.recordValue(value);
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
            Var variable = set.variable();
            frame[variable.index()] = value;
            variable.profile.recordValue(value);
            return value;
        }

        @Override
        public Object visitVar(Var var) {
            return frame[var.index()];
        }
    }

    /*
        Instance
     */

    public Object interpret(Method method) {
        Object[] frame = new Object[method.localsCount()];
        method.profile.recordInvocation(frame);
        try {
            return method.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Method method, Object arg) {
        Object[] frame = new Object[method.localsCount()];
        frame[0] = arg;
        method.profile.recordInvocation(frame);
        try {
            return method.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Method method, Object arg1, Object arg2) {
        Object[] frame = new Object[method.localsCount()];
        frame[0] = arg1;
        frame[1] = arg2;
        method.profile.recordInvocation(frame);
        try {
            return method.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpretWithArgs(Method method, Object[] actualArguments) {
        Object[] frame = new Object[method.localsCount()];
        System.arraycopy(actualArguments, 0, frame, 0, actualArguments.length);
        method.profile.recordInvocation(frame);
        try {
            return method.body().accept(new ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

}
