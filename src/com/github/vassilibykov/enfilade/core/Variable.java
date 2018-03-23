// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.Nullable;

/**
 * A variable definition in a function or a let form. Not to be confused with
 * {@link VarRef}, which is an atomic expression referencing a variable.
 * Note that a definition is not an expression.
 */
public class Variable {
    /** Variable name, for debugging only. Has no semantic effect. */
    @Nullable private final String name;
    /*internal*/ int index = -1;
    /*internal*/ final ValueProfile profile = new ValueProfile();
    /*internal*/ final CompilerAnnotation compilerAnnotation = new CompilerAnnotation();

    Variable(@Nullable String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public int index() {
        return index;
    }

    @Override
    public String toString() {
        return name != null ? name : "var" + hashCode();
    }
}
