// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.UserFunction;
import com.github.vassilibykov.trifle.expression.Lambda;

import java.lang.invoke.SwitchPoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple mock-up of a Smalltalk class sans inheritance to use in tests.
 * Note the synchronization of {@link #invalidationSwitchPoint()},
 * {@link #defineMethod(String, Lambda)} and {@link #removeMethod(String)}.
 * It's required not because of the access to the method dictionary, but
 * because of the switch point.
 */
class ToySmalltalkClass {
    private final FixedObjectDefinition definition;
    private final Map<String, UserFunction> methodDictionary;
    private SwitchPoint invalidationSwitchPoint;

    ToySmalltalkClass(List<String> fieldNames) {
        this.definition = new FixedObjectDefinition(fieldNames);
        this.methodDictionary = new ConcurrentHashMap<>();
        this.invalidationSwitchPoint = new SwitchPoint();
    }

    public FixedObjectDefinition definition() {
        return definition;
    }

    public synchronized SwitchPoint invalidationSwitchPoint() {
        return invalidationSwitchPoint;
    }

    public ToySmalltalkObject newInstance() {
        return new ToySmalltalkObject(this);
    }

    public synchronized void defineMethod(String selector, Lambda definition) {
        var oldValue = methodDictionary.put(selector, UserFunction.construct(selector, definition));
        if (oldValue != null) {
            SwitchPoint.invalidateAll(new SwitchPoint[]{invalidationSwitchPoint});
            invalidationSwitchPoint = new SwitchPoint();
        }
    }

    public synchronized void removeMethod(String selector) {
        if (methodDictionary.remove(selector) != null) {
            SwitchPoint.invalidateAll(new SwitchPoint[]{invalidationSwitchPoint});
            invalidationSwitchPoint = new SwitchPoint();
        }
    }

    public UserFunction lookupMethod(String selector) {
        return methodDictionary.get(selector);
    }
}
