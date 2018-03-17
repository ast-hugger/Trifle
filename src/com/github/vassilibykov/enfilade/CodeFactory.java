package com.github.vassilibykov.enfilade;

import java.util.function.*;

public class CodeFactory {

    public static Method nullaryMethod(Supplier<Expression> bodyMaker) {
        return Method.with(new Var[0], bodyMaker.get());
    }

    public static Method unaryMethod(Function<Var, Expression> bodyMaker) {
        Var arg = var("a1");
        return Method.with(new Var[]{arg}, bodyMaker.apply(arg));
    }

    public static Method binaryMethod(BiFunction<Var, Var, Expression> bodyMaker) {
        Var arg1 = var("a1");
        Var arg2 = var("a2");
        return Method.with(new Var[]{arg1, arg2}, bodyMaker.apply(arg1, arg2));
    }

    public static Call1 call(Method method, AtomicExpression arg) {
        return new Call1(method, arg);
    }

    public static Call2 call(Method method, AtomicExpression arg1, AtomicExpression arg2) {
        return new Call2(method, arg1, arg2);
    }

    public static Const const_(Object value) {
        return new Const(value);
    }

    public static If if_(AtomicExpression condition, Expression trueBranch, Expression falseBranch) {
        return new If(condition, trueBranch, falseBranch);
    }

    public static Let let(Var variable, Expression initializer, Expression body) {
        return new Let(variable, initializer, body);
    }

    public static Primitive1 primitive(UnaryOperator<Object> function, AtomicExpression argument) {
        return new Primitive1.Wrapper(function, argument);
    }

    public static Primitive2 primitive(BinaryOperator<Object> function, AtomicExpression arg1, AtomicExpression arg2) {
        return new Primitive2.Wrapper(function, arg1, arg2);
    }

    public static Prog prog(Expression... expressions) {
        return new Prog(expressions);
    }

    public static Ret ret(AtomicExpression value) {
        return new Ret(value);
    }

    public static SetVar set(Var variable, AtomicExpression value) {
        return new SetVar(variable, value);
    }

    public static Var var(String name) {
        return new Var(name);
    }

}
