// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

public abstract class AstNode {

    public interface Visitor<T> {
        T visitAssignment(Assignment assignment);
        T visitBlock(Block block);
        T visitClassDeclaration(ClassDeclaration classDeclaration);
        T visitLiteral(Literal literal);
        T visitMessageSend(MessageSend messageSend);
        T visitMethodDeclaration(MethodDeclaration methodDeclaration);
        T visitReturn(Return aReturn);
        T visitSourceUnit(SourceUnit sourceUnit);
        T visitVarReference(VarReference varReference);
    }

    public abstract <T> T accept(Visitor<T> visitor);
}
