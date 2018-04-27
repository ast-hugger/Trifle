// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import java.lang.invoke.SwitchPoint;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A definition of fields available in a {@link FixedObject} which follows this
 * definition. The set of field names may be changed dynamically.
 */
public class FixedObjectDefinition {

    private FixedObjectLayout layout;
    /**
     * Guards all access to this object and the stability of its layout. An
     * explicit lock is used instead of the intrinsic monitor to support
     * transactional atomic modifications when a FixedObjectDefinition is a
     * foundation of a class, and a modification affects multiple classes in a
     * hierarchy.
     */
    private final Lock lock = new ReentrantLock();

    public FixedObjectDefinition(List<String> fieldNames) {
        this.layout = new FixedObjectLayout(fieldNames);
    }

    public FixedObjectLayout layout() {
        lock();
        try {
            return layout;
        } finally {
            unlock();
        }
    }

    public List<String> fieldNames() {
        lock();
        try {
            return layout.fieldNames();
        } finally {
            unlock();
        }
    }

    public void setFieldNames(List<String> fieldNames) {
        lock();
        try {
            if (layout.fieldNames().equals(fieldNames)) return;
            var oldLayout = layout;
            layout = new FixedObjectLayout(fieldNames);
            SwitchPoint.invalidateAll(new SwitchPoint[] {oldLayout.switchPoint()});
        } finally {
            unlock();
        }
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public FixedObject instantiate() {
        return new FixedObject(this);
    }

    @SuppressWarnings("unused") // called by generated code
    public Object instantiateAsObject() {
        return new FixedObject(this);
    }
}
