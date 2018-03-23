// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An annotation associated with an expression by the compiler analyzer. Holds
 * whatever information the compiler wants to associate with that expression.
 */
public class CompilerAnnotation {
    private ExpressionType inferredType;
    private ExpressionType observedType;

    CompilerAnnotation() {}

    public synchronized ExpressionType inferredType() {
        return Objects.requireNonNull(inferredType, "types have not been inferred yet");
    }

    public synchronized ExpressionType observedType() {
        return Objects.requireNonNull(observedType, "observed types have not been recorded yet");
    }

    /**
     * Return a type the expression should be assumed to produced while
     * generating specialized code. Because specialized code is opportunistic,
     * observed type trumps the inferred type because it's potentially more
     * specific, even if in general incorrect.
     */
    public synchronized TypeCategory specializationType() {
        return observedType.typeCategory()
            .orElseGet(() -> inferredType.typeCategory()
                .orElse(TypeCategory.REFERENCE));
    }

    /*internal*/ synchronized void setInferredType(@NotNull ExpressionType expressionType) {
        this.inferredType = expressionType;
    }

    /**
     * Replace the inferred type of this annotation with the union of the
     * specified inferred type and the current one. Return a boolean indicating
     * whether the unified inferred type is different from the original.
     */
    /*internal*/ synchronized boolean unifyInferredTypeWith(ExpressionType type) {
        ExpressionType newType = inferredType.union(type);
        boolean changed = !inferredType.equals(newType);
        inferredType = newType;
        return changed;
    }

    /*internal*/ synchronized void setObservedType(@NotNull ExpressionType type) {
        observedType = type;
    }

    /*internal*/ synchronized boolean unifyObservedTypeWith(ExpressionType type) {
        ExpressionType newType = observedType.union(type);
        boolean changed = !observedType.equals(newType);
        observedType = newType;
        return changed;
    }

    @Override
    public String toString() {
        return "Inferred: " + inferredType + ", observed: " + observedType;
    }
}
