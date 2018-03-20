package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

/**
 * An annotation associated with an expression by the compiler analyzer.
 * Summarizes the values the expression can evaluate to, as observed by the
 * profiling interpreter.
 */
public class CompilerAnnotation {
    @NotNull private final TypeCategory typeCategory;

    CompilerAnnotation(@NotNull TypeCategory typeCategory) {
        this.typeCategory = typeCategory;
    }

    /**
     * The category of values this expression has been known to evaluate to.
     */
    public TypeCategory valueCategory() {
        return typeCategory;
    }
}
