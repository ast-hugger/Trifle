// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.core.Invocable;
import com.github.vassilibykov.trifle.object.MessageDispatchExtension;
import com.github.vassilibykov.trifle.object.MessageSend;

import java.math.BigInteger;
import java.util.Optional;

class MessageDispatchCustomizations implements MessageDispatchExtension {

    static void install() {
        MessageSend.installDispatchExtension(new MessageDispatchCustomizations());
    }

    private MessageDispatchCustomizations() {}

    @Override
    public Optional<Invocable> lookupStrangeReceiverSelector(String selector, Object[] args) {
        var receiver = args[0];
        if (receiver instanceof Boolean) {
            var klass = (Boolean) receiver ? Smalltalk.TRUE_CLASS : Smalltalk.FALSE_CLASS;
            return Optional.ofNullable(klass.lookupSelector(selector));
        } else if (receiver instanceof Integer || receiver instanceof BigInteger) {
            return Optional.ofNullable(Smalltalk.INTEGER_CLASS.lookupSelector(selector));
        } else if (receiver instanceof String) {
            return Optional.ofNullable(Smalltalk.STRING_CLASS.lookupSelector(selector));
        } else if (receiver == null) {
            return Optional.ofNullable(Smalltalk.UNDEFINED_OBJECT_CLASS.lookupSelector(selector));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Object messageNotUnderstood(String selector, Object[] args) {
        throw new RuntimeException("message not understood: " + selector);
    }
}
