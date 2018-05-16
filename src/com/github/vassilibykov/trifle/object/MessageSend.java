// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.expression.Callable;
import com.github.vassilibykov.trifle.expression.Visitor;

/**
 * A callable of a {@link com.github.vassilibykov.trifle.expression.Call}
 * expression with the core semantics of the Smalltalk message send. The
 * receiver (first argument) of such a call is typically a {@link MessageReceiver}.
 * Alternatively, a {@link MessageDispatchExtension} can be
 * installed with this class to implement message dispatch logic for receivers
 * which are not instances of {@link MessageReceiver}.
 */
public class MessageSend implements Callable {

    /**
     * Create a new instance with the specified selector.
     */
    public static MessageSend selector(String selector) {
        return new MessageSend(selector);
    }

    public static void installDispatchExtension(MessageDispatchExtension extension) {
        MessageSendDispatcher.installExtension(extension);
    }

    /*
        Instance side
     */

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
