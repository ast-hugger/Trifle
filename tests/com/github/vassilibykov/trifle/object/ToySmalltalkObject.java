// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.Invocable;

import java.lang.invoke.SwitchPoint;
import java.util.Optional;

/**
 * The essence of a Smalltalk instance to go with {@link ToySmalltalkClass}.
 */
class ToySmalltalkObject extends FixedObject implements MessageReceiver {
    private final ToySmalltalkClass klass;

    ToySmalltalkObject(ToySmalltalkClass klass) {
        super(klass.definition());
        this.klass = klass;
    }

    @Override
    public Optional<? extends Invocable> lookupSelector(String messageSelector) {
        return Optional.ofNullable(klass.lookupMethod(messageSelector));
    }

    @Override
    public Object behaviorToken() {
        return klass;
    }

    @Override
    public SwitchPoint invalidationSwitchPoint() {
        return klass.invalidationSwitchPoint();
    }
}
