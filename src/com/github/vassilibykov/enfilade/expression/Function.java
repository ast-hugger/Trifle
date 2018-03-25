// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Function {
    public static Function with(List<Variable> arguments, Expression body) {
        return new Function(arguments, body);
    }

    public static Function recursive(List<Variable> arguments, java.util.function.Function<Function, Expression> bodyBuilder) {
        return new Function(arguments, bodyBuilder);
    }

    public static Function recursive(Variable arguments, java.util.function.Function<Function, Expression> bodyBuilder) {
        return new Function(List.of(arguments), bodyBuilder);
    }

    @NotNull private final List<Variable> arguments;
    @NotNull private final Expression body;

    Function(@NotNull List<Variable> arguments, @NotNull Expression body) {
        this.arguments = arguments;
        this.body = body;
    }

    Function(@NotNull List<Variable> arguments, @NotNull java.util.function.Function<Function, Expression> bodyBuilder) {
        this.arguments = arguments;
        this.body = bodyBuilder.apply(this);
    }

    public List<Variable> arguments() {
        return arguments;
    }

    public Expression body() {
        return body;
    }
}
