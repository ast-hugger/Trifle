// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

/**
 * Thrown in generated code when the result of the preceding computation
 * currently on the stack cannot be accepted by its continuation because the
 * continuation if of a narrower type. The value is wrapped as the {@link
 * #value} field of this exception. The structure of the {@link EvaluatorNode}
 * language allows us to generate code in such a way that there are no
 * other values on the stack at this point.
 *
 * <p>This exception can only occur in generated specialized code when execution
 * takes a path not taken while profiling. For example, a function defined as
 * (for simplicity, in Javascript syntax rather than as an equivalent {@code
 * Expression} tree):
 *
 * <pre>{@code
 * function fib(n) {
 *     if (n < 0) {
 *         return "error";
 *     } else if (n < 2) {
 *         return 1;
 *     } else {
 *         return fib(n - 1) + fib(n - 2)
 *     }
 * }
 * }</pre>
 *
 * would be in this situation it it were profiled with non-negative arguments,
 * and later received a negative argument while running its specialized compiled
 * form whose signature is {@code (int)int}.
 *
 * <p>More boringly, this might be called a {@code SpecializationFailureException},
 * but the current continuation corresponds to the "hole" in the formal language
 * of evaluation contexts, so a value incompatible with the current continuation
 * type is quite literally a square peg in a round hole.
 */
public class SquarePegException extends RuntimeException {
    public static final String INTERNAL_CLASS_NAME = GhostWriter.internalClassName(SquarePegException.class);

    public static SquarePegException with(Object value) {
        return new SquarePegException(value);
    }

    public final Object value;

    private SquarePegException(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }
}
