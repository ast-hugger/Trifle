// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A set of static factory methods for creating expressions in a readable form,
 * especially in unit tests.
 */
public class ExpressionLanguage {

    public static Lambda nullaryFunction(Supplier<Expression> bodyBuilder) {
        return Lambda.with(List.of(), bodyBuilder.get());
    }

    public static Lambda unaryFunction(java.util.function.Function<Variable, Expression> bodyBuilder) {
        Variable arg = var("a1");
        return Lambda.with(List.of(arg), bodyBuilder.apply(arg));
    }

    public static Lambda binaryFunction(BiFunction<Variable, Variable, Expression> bodyBuilder) {
        Variable arg1 = var("a1");
        Variable arg2 = var("a2");
        return Lambda.with(List.of(arg1, arg2), bodyBuilder.apply(arg1, arg2));
    }

    public static Call call(Lambda function) {
        return Call.with(function);
    }

    public static Call call(Lambda function, AtomicExpression arg) {
        return Call.with(function, arg);
    }

    public static Call call(Lambda function, AtomicExpression arg1, AtomicExpression arg2) {
        return Call.with(function, arg1, arg2);
    }

    public static Const const_(Object value) {
        return Const.value(value);
    }

    public static If if_(AtomicExpression condition, Expression trueBranch, Expression falseBranch) {
        return If.with(condition, trueBranch, falseBranch);
    }

    public static Let let(Variable variable, Expression initializer, Expression body) {
        return Let.with(variable, initializer, body);
    }

    public static Block prog(Expression... expressions) {
        return Block.with(List.of(expressions));
    }

    public static Return ret(AtomicExpression value) {
        return Return.with(value);
    }

    public static SetVariable set(Variable variable, AtomicExpression value) {
        return SetVariable.with(variable, value);
    }

    public static Variable var(String name) {
        return Variable.named(name);
    }

}
