package com.github.vassilibykov.enfilade.compiler;

import org.jetbrains.annotations.NotNull;

public class CompilerAnnotation {

    @NotNull private final ValueCategory valueCategory;

    CompilerAnnotation(@NotNull ValueCategory valueCategory) {
        this.valueCategory = valueCategory;
    }

    public ValueCategory valueCategory() {
        return valueCategory;
    }
}
