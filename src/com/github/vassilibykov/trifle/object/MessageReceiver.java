// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.Invocable;

import java.lang.invoke.SwitchPoint;
import java.util.Optional;

/**
 * Should be implemented by any object that may appear as the receiver (first
 * argument) of a call with {@link MessageSend} as the callable.
 */
public interface MessageReceiver {

    /**
     * Look up a method to handle a message.
     *
     * @param messageSelector The message selector as it appears in the
     *        {@link MessageSend} callable or its compiled form.
     * @return An invocable to invoke to handle the message.
     */
    Optional<? extends Invocable> lookupSelector(String messageSelector);

    /**
     * Return an object to be used as inline cache management key. A behavior
     * token is an arbitrary object which identifies a set of mappings of
     * selectors to invocables, as exposed by {@link #lookupSelector(String)}.
     * After a successful selector lookup, a call site may cache the result and
     * use it for any subsequent message receiver which has an identical behavior
     * token.
     */
    Object behaviorToken();

    /**
     * Return a switch point invokedynamic instructions will use for inline
     * cache invalidation. The implementor of this interface must guarantee that
     * the switch point returned by this method is unique for a particular
     * {@link #behaviorToken() behavior token}, and will be invalidated as soon
     * as the mapping of selectors to invocables, as manifested by {@link
     * #lookupSelector(String)}, changes for that token.
     */
    SwitchPoint invalidationSwitchPoint();
}
