package com.github.vassilibykov.enfilade.core;

public class Interpreter {
    public static final Interpreter INSTANCE = new Interpreter();

    static class ReturnException extends RuntimeException {
        final Object value;

        ReturnException(Object value) {
            this.value = value;
        }
    }

    static class Evaluator implements Expression.Visitor<Object> {
        protected final Object[] frame;

        Evaluator(Object[] frame) {
            this.frame = frame;
        }

        @Override
        public Object visitCall0(Call0 call) {
            return call.function().nexus.invoke();
        }

        @Override
        public Object visitCall1(Call1 call) {
            Object arg = call.arg().accept(this);
            return call.function().nexus.invoke(arg);
        }

        @Override
        public Object visitCall2(Call2 call) {
            Object arg1 = call.arg1().accept(this);
            Object arg2 = call.arg2().accept(this);
            return call.function().nexus.invoke(arg1, arg2);
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

    public Object interpret(Function function) {
        Object[] frame = new Object[function.localsCount()];
        try {
            return function.body().accept(new ProfilingInterpreter.ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Function function, Object arg) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg;
        try {
            return function.body().accept(new ProfilingInterpreter.ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpret(Function function, Object arg1, Object arg2) {
        Object[] frame = new Object[function.localsCount()];
        frame[0] = arg1;
        frame[1] = arg2;
        try {
            return function.body().accept(new ProfilingInterpreter.ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }

    public Object interpretWithArgs(Function function, Object[] actualArguments) {
        Object[] frame = new Object[function.localsCount()];
        System.arraycopy(actualArguments, 0, frame, 0, actualArguments.length);
        try {
            return function.body().accept(new ProfilingInterpreter.ProfilingEvaluator(frame));
        } catch (ReturnException e) {
            return e.value;
        }
    }
}
