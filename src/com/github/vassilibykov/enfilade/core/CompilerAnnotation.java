// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * An annotation associated with an expression by the compiler analyzer.
 * Summarizes the values the expression can evaluate to, as observed by the
 * profiling interpreter and/or inferred by the analyzer.
 *
 * <p>If in future the compiler needs to associate more information with
 * expressions as it compiles them, this is the place to do that.
 */
public class CompilerAnnotation {
    @NotNull private final TypeCategory typeCategory;

    CompilerAnnotation(@NotNull TypeCategory typeCategory) {
        this.typeCategory = typeCategory;
    }

    /**
     * The category of values this expression has been observed to evaluate to,
     * or inferred by the analyzer.
     */
    public TypeCategory valueCategory() {
        return typeCategory;
    }
}
