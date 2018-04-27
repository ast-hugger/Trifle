// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.expression.Callable;
import com.github.vassilibykov.trifle.expression.Visitor;

public class MessageSend implements Callable {

    public static MessageSend selector(String selector) {
        return new MessageSend(selector);
    }

    private final String selector;

    private MessageSend(String selector) {
        this.selector = selector;
    }

    public String selector() {
        return selector;
    }

    @Override
    public CallDispatcher createDispatcher(Visitor<EvaluatorNode> translator) {
        return new MessageSendDispatcher(selector);
    }
}
