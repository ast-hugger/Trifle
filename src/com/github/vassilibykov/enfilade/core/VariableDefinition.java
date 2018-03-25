// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * A variable definition in a function or a let form. Not to be confused with
 * {@link VariableReferenceNode}, which is an atomic expression referencing a variable.
 * Note that a definition is not an expression.
 */
public class VariableDefinition {
    @NotNull private final Variable definition;
    /*internal*/ int index = -1;
    /*internal*/ final ValueProfile profile = new ValueProfile();
    /*internal*/ final CompilerAnnotation compilerAnnotation = new CompilerAnnotation();

    VariableDefinition(@NotNull Variable definition) {
        this.definition = definition;
    }

    public String name() {
        return definition.name();
    }

    public int index() {
        return index;
    }

    @Override
    public String toString() {
        return name();
    }
}
