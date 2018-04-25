// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.object;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A list of field names available in a {@link FixedObject} which follows
 * this definition. The set of field names may be changed dynamically.
 */
public class FixedObjectDefinition {
    private FixedObjectLayout layout;
    /**
     * Must be acquired and held by any operation that replaces the layout,
     * until the old layout's switch point has been invalidated. Must also be
     * acquired while constructing a fast access path at an access site (which
     * will incorporate a dependency on the current layout's switch point).
     */
    private final Lock modificationLock = new ReentrantLock();

    public synchronized List<String> fieldNames() {
        return layout.fieldNames();
    }

    public void setFieldNames(List<String> fieldNames) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }
}
