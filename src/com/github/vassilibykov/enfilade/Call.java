package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Call extends ComplexExpression {
    @NotNull private final AtomicExpression function; // FIXME the function is actually an exolinguistic selector
    @NotNull private final AtomicExpression[] arguments;

    Call(@NotNull AtomicExpression function, @NotNull AtomicExpression[] arguments) {
        this.function = function;
        this.arguments = arguments;
    }

    public AtomicExpression function() {
        return function;
    }

    public AtomicExpression[] arguments() {
        return arguments;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall(this);
    }

    @Override
    public String toString() {
        return "(call " + function + Stream.of(arguments).map(Objects::toString).collect(Collectors.joining(" ")) + ")";
    }
}
