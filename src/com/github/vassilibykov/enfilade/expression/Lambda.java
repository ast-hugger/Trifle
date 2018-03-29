// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Lambda extends AtomicExpression {
    public static Lambda with(List<Variable> arguments, Expression body) {
        return new Lambda(arguments, body);
    }

    public static Lambda recursive(List<Variable> arguments, Function<Const, Expression> bodyBuilder) {
        return new Lambda(arguments, bodyBuilder);
    }

    public static Lambda recursive(Variable arguments, Function<Const, Expression> bodyBuilder) {
        return new Lambda(List.of(arguments), bodyBuilder);
    }

    @NotNull private final List<Variable> arguments;
    @NotNull private final Expression body;

    private Lambda(@NotNull List<Variable> arguments, @NotNull Expression body) {
        this.arguments = arguments;
        this.body = body;
    }

    private Lambda(@NotNull List<Variable> arguments, @NotNull Function<Const, Expression> bodyBuilder) {
        this.arguments = arguments;
        this.body = bodyBuilder.apply(Const.value(this));
    }

    public List<Variable> arguments() {
        return arguments;
    }

    public Expression body() {
        return body;
    }

    @Override
    public String toString() {
        return "lambda(["
            + arguments.stream().map(each -> each.toString()).collect(Collectors.joining(" ,"))
            + "] "
            + body
            + ")";
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLambda(this);
    }
}
