// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ClassDeclaration extends AstNode {

    @NotNull private final String name;
    @NotNull private final String superclassName;
    @NotNull private final List<String> instVarNames;

    ClassDeclaration(@NotNull String name, @NotNull String superclassName, List<String> instVarNames) {
        this.name = name;
        this.superclassName = superclassName;
        this.instVarNames = Collections.unmodifiableList(instVarNames);
    }

    public String name() {
        return name;
    }

    public String superclassName() {
        return superclassName;
    }

    public List<String> instVarNames() {
        return instVarNames;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitClassDeclaration(this);
    }
}
