// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Lambda {
    public static Lambda with(List<Variable> arguments, Expression body) {
        return new Lambda(arguments, body);
    }

    public static Lambda recursive(List<Variable> arguments, java.util.function.Function<Lambda, Expression> bodyBuilder) {
        return new Lambda(arguments, bodyBuilder);
    }

    public static Lambda recursive(Variable arguments, java.util.function.Function<Lambda, Expression> bodyBuilder) {
        return new Lambda(List.of(arguments), bodyBuilder);
    }

    @NotNull private final List<Variable> arguments;
    @NotNull private final Expression body;

    Lambda(@NotNull List<Variable> arguments, @NotNull Expression body) {
        this.arguments = arguments;
        this.body = body;
    }

    Lambda(@NotNull List<Variable> arguments, @NotNull java.util.function.Function<Lambda, Expression> bodyBuilder) {
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
