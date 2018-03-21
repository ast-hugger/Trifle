// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.primitives.*;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A set of static factory methods for creating expressions in a readable form,
 * especially in unit tests.
 */
public class ExpressionLanguage {

    public static Function nullaryFunction(Supplier<Expression> bodyBuilder) {
        return Function.with(new Var[0], bodyBuilder.get());
    }

    public static Function unaryFunction(java.util.function.Function<Var, Expression> bodyBuilder) {
        Var arg = var("a1");
        return Function.with(new Var[]{arg}, bodyBuilder.apply(arg));
    }

    public static Function binaryFunction(BiFunction<Var, Var, Expression> bodyBuilder) {
        Var arg1 = var("a1");
        Var arg2 = var("a2");
        return Function.with(new Var[]{arg1, arg2}, bodyBuilder.apply(arg1, arg2));
    }

    public static Call0 call(Function function) {
        return new Call0(function);
    }

    public static Call1 call(Function function, AtomicExpression arg) {
        return new Call1(function, arg);
    }

    public static Call2 call(Function function, AtomicExpression arg1, AtomicExpression arg2) {
        return new Call2(function, arg1, arg2);
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

    public static Add add(AtomicExpression arg1, AtomicExpression arg2) {
        return new Add(arg1, arg2);
    }

    public static LessThan lessThan(AtomicExpression arg1, AtomicExpression arg2) {
        return new LessThan(arg1, arg2);
    }

    public static Mul mul(AtomicExpression arg1, AtomicExpression arg2) {
        return new Mul(arg1, arg2);
    }

    public static Sub sub(AtomicExpression arg1, AtomicExpression arg2) {
        return new Sub(arg1, arg2);
    }

    public static Primitive1 negate(AtomicExpression arg) {
        return new Negate(arg);
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