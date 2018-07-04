// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.core.Invocable;
import com.github.vassilibykov.trifle.object.FixedObject;
import com.github.vassilibykov.trifle.object.MessageReceiver;

import java.lang.invoke.SwitchPoint;
import java.util.Optional;

public class SmalltalkObject extends FixedObject implements MessageReceiver {

    private final SmalltalkClass smalltalkClass;

    SmalltalkObject(SmalltalkClass smalltalkClass) {
        super(smalltalkClass.definition());
        this.smalltalkClass = smalltalkClass;

    }

    SmalltalkClass smalltalkClass() {
        return smalltalkClass;
    }

    @Override
    public Optional<? extends Invocable> lookupSelector(String selector) {
        return Optional.ofNullable(smalltalkClass.lookupSelector(selector));
    }

    @Override
    public Object behaviorToken() {
        return smalltalkClass;
    }

    @Override
    public SwitchPoint invalidationSwitchPoint() {
        return smalltalkClass.invalidationSwitchPoint();
    }

    public Object perform(String selector, Object... args) {
        var argsWithSelf = new Object[args.length + 1];
        argsWithSelf[0] = this;
        System.arraycopy(args, 0, argsWithSelf, 1, args.length);
        return lookupSelector(selector).orElseThrow().invokeWithArguments(argsWithSelf);
    }
}
