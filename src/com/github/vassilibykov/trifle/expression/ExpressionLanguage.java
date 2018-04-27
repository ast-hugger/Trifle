// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

import com.github.vassilibykov.trifle.core.FreeFunction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A set of static factory methods for creating expressions in a readable form,
 * especially in unit tests.
 */
public class ExpressionLanguage {

    private static int argSerial = 0;
    private static int tempSerial = 0;

    public static Lambda lambda(Supplier<Expression> bodyBuilder) {
        return Lambda.with(List.of(), bodyBuilder.get());
    }

    public static Lambda lambda(Function<Variable, Expression> bodyBuilder) {
        var arg = var("a" + argSerial++);
        return Lambda.with(List.of(arg), bodyBuilder.apply(arg));
    }

    public static Lambda lambda(BiFunction<Variable, Variable, Expression> bodyBuilder) {
        var arg1 = var("a" + argSerial++);
        var arg2 = var("a" + argSerial++);
        return Lambda.with(List.of(arg1, arg2), bodyBuilder.apply(arg1, arg2));
    }

    public static Call call(Callable function) {
        return Call.with(function);
    }

    public static Call call(Callable function, AtomicExpression arg) {
        return Call.with(function, arg);
    }

    public static Call call(Callable function, AtomicExpression arg1, AtomicExpression arg2) {
        return Call.with(function, arg1, arg2);
    }

    public static Const const_(Object value) {
        return Const.value(value);
    }

    public static FreeFunctionReference direct(FreeFunction target) {
        return FreeFunctionReference.to(target);
    }

    public static If if_(AtomicExpression condition, Expression trueBranch, Expression falseBranch) {
        return If.with(condition, trueBranch, falseBranch);
    }

    public static Let let(Variable variable, Expression initializer, Expression body) {
        return Let.with(variable, initializer, body);
    }

    public static Block block(Expression... expressions) {
        return Block.with(List.of(expressions));
    }

    public static PrimitiveCall primitive(Class<? extends Primitive> primitive, AtomicExpression... args) {
        return PrimitiveCall.with(primitive, args);
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

    public static Let bind(Expression initializer, Function<Variable, Expression> bodyBuilder) {
        var var = var("t" + tempSerial++);
        return Let.with(var, initializer, bodyBuilder.apply(var));
    }
}
