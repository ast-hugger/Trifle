// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.grammar;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MessageSend extends AstNode {

    @NotNull private final AstNode receiver;
    @NotNull private final String selector;
    @NotNull private final List<AstNode> arguments;

    MessageSend(@NotNull AstNode receiver, @NotNull String selector, @NotNull List<AstNode> arguments) {
        this.receiver = receiver;
        this.selector = selector;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    public AstNode receiver() {
        return receiver;
    }

    public String selector() {
        return selector;
    }

    public List<AstNode> arguments() {
        return arguments;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitMessageSend(this);
    }
}
