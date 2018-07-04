// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SourceUnit extends AstNode {

    @NotNull private final ClassDeclaration classDeclaration;
    @NotNull private final List<MethodDeclaration> instanceMethods;
    @NotNull private final List<MethodDeclaration> classMethods;

    SourceUnit(@NotNull ClassDeclaration classDeclaration, @NotNull List<MethodDeclaration> instanceMethods, @NotNull List<MethodDeclaration> classMethods) {
        this.classDeclaration = classDeclaration;
        this.instanceMethods = Collections.unmodifiableList(instanceMethods);
        this.classMethods = Collections.unmodifiableList(classMethods);
    }

    public ClassDeclaration classDeclaration() {
        return classDeclaration;
    }

    public List<MethodDeclaration> instanceMethods() {
        return instanceMethods;
    }

    public List<MethodDeclaration> classMethods() {
        return classMethods;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSourceUnit(this);
    }
}
