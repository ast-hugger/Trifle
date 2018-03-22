// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.Objects;
import java.util.Optional;

/**
 * An annotation associated with an expression by the compiler analyzer.
 * Summarizes the values the expression can evaluate to, as observed by the
 * profiling interpreter and/or inferred by the analyzer.
 *
 * <p>If in future the compiler needs to associate more information with
 * expressions as it compiles them, this is the place to do that.
 */
public class CompilerAnnotation {
    private InferredType inferredType;
    private TypeCategory observedType;

    CompilerAnnotation() {}

    /**
     * The category of values this expression has been observed to evaluate to,
     * or inferred by the analyzer.
     */
    @Deprecated
    public TypeCategory valueCategory() {
        return observedType;
    }

    public synchronized InferredType inferredType() {
        return Objects.requireNonNull(inferredType, "types have not been inferred yet");
    }

    /*internal*/ synchronized void setInferredType(InferredType inferredType) {
        this.inferredType = inferredType;
    }

    /*internal*/ synchronized void unifyInferredTypeWith(InferredType type) {
        inferredType = inferredType.union(type);
    }

    public synchronized Optional<TypeCategory> observedType() {
        return Optional.ofNullable(observedType);
    }

    /*internal*/ synchronized void setObservedType(TypeCategory type) {
        observedType = type;
    }
}
