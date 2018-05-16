// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.Invocable;

import java.util.Optional;

/**
 * An implementor of this interface can be registered with {@link MessageSend}
 * to customize message dispatch behavior in situations when a message dispatch
 * cannot proceed normally.
 *
 * @see MessageSend#installDispatchExtension(MessageDispatchExtension)
 */
public interface MessageDispatchExtension {
    /**
     * Handle message dispatch for the case when the receiver is not a
     * {@link MessageReceiver}. This allows granting message dispatch
     * powers to instances of classes we have no control of, such as {@link
     * Integer}. If there is no invocable to handle the selector, the
     * implementation must return an empty optional and handle the failure
     * separately in {@link #messageNotUnderstood}.
     *
     * @param selector The selector of the message send.
     * @param arguments All arguments of the message send (including the receiver).
     */
    Optional<Invocable> lookupStrangeReceiverSelector(String selector, Object[] arguments);

    /**
     * Called if an earlier call of {@link #lookupStrangeReceiverSelector(String, Object[])}
     * returned an empty optional, indicating that there is no invocable to invoke to handle
     * the selector. This method may return normally with the result of the message send,
     * or throw an exception to indicate a failure.
     *
     * @param selector The selector which wasn't understood.
     * @param arguments All arguments of the message send whose selector wasn't understood,
     *        including the receiver.
     * @return An object to return from the message send as the result.
     */
    Object messageNotUnderstood(String selector, Object[] arguments);
}
