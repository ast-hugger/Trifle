// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.core.Invocable;
import com.github.vassilibykov.trifle.core.UserFunction;
import com.github.vassilibykov.trifle.expression.Lambda;
import com.github.vassilibykov.trifle.object.FixedObjectDefinition;

import java.lang.invoke.SwitchPoint;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SmalltalkClass {

    private final FixedObjectDefinition definition;
    private final SmalltalkClass superclass;
    private final List<WeakReference<SmalltalkClass>> subclasses;
    private final Map<String, Invocable> methodDictionary;
    private SwitchPoint invalidationSwitchPoint;
    private final Lock updateLock;

    SmalltalkClass(SmalltalkClass superclass, List<String> instVarNames) {
        this.definition = new FixedObjectDefinition(instVarNames);
        this.superclass = superclass;
        this.subclasses = new ArrayList<>();
        this.methodDictionary = new ConcurrentHashMap<>();
        this.invalidationSwitchPoint = new SwitchPoint();
        this.updateLock = new ReentrantLock();
    }

    public FixedObjectDefinition definition() {
            return definition;
    }

    /**
     * The names of instance variables defined in this class.
     * Inherited names are not included.
     */
    public List<String> localInstanceVariableNames() {
        return definition.fieldNames();
    }

    /**
     * The names of instance variables defined in this class
     * and inherited from superclasses, inherited first.
     */
    public List<String> allInstanceVariableNames() {
        var result = new ArrayList<String>();
        addInstanceVariableNamesTo(result);
        return result;
    }

    private void addInstanceVariableNamesTo(List<String> list) {
        if (superclass != null) superclass.addInstanceVariableNamesTo(list);
        list.addAll(localInstanceVariableNames());
    }

    public List<SmalltalkClass> subclasses() {
        return this.subclasses.stream()
            .map(Reference::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    synchronized SwitchPoint invalidationSwitchPoint() {
        updateLock.lock();
        try {
            return invalidationSwitchPoint;
        } finally {
            updateLock.unlock();
        }
    }

    public void addSubclass(Supplier<SmalltalkClass> factory) {
        updateLock.lock();
        try {
            subclasses.add(new WeakReference<>(factory.get()));
        } finally {
            updateLock.unlock();
        }
    }

    private void lockDescendants() {
        // The lock on this class is already being held.
        var subclasses = subclasses();
        subclasses.forEach(each -> each.updateLock.lock());
        subclasses.forEach(each -> each.lockDescendants());
    }

    private void unlockDescendants() {
        // The lock on this class is still being held.
        var subclasses = subclasses();
        subclasses.forEach(each -> each.unlockDescendants());
        subclasses.forEach(each -> each.updateLock.unlock());
    }

    public SmalltalkObject newInstance() {
        return new SmalltalkObject(this);
    }

    // FIXME update to account for inheritance
    public void installMethod(String selector, Invocable method) {
        updateLock.lock();
        try {
            var oldMethod = methodDictionary.put(selector, method);
            if (oldMethod != null) {
                SwitchPoint.invalidateAll(new SwitchPoint[]{invalidationSwitchPoint});
                invalidationSwitchPoint = new SwitchPoint();
            }
        } finally {
            updateLock.unlock();
        }
    }

    // FIXME update to account for inheritance
    public synchronized void removeMethod(String selector) {
        if (methodDictionary.remove(selector) != null) {
            SwitchPoint.invalidateAll(new SwitchPoint[]{invalidationSwitchPoint});
            invalidationSwitchPoint = new SwitchPoint();
        }
    }

    Invocable lookupSelector(String selector) {
        var method = methodDictionary.get(selector);
        return method == null && superclass != null ? superclass.lookupSelector(selector) : method;
    }
}
