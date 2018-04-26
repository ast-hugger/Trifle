// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.RuntimeError;

/**
 * An object with storage locations (fields), identified by names (strings).
 * It is "fixed" in the sense that the set of fields of an object is determined
 * by its definition, and cannot be changed for a particular object without
 * changing it for all other objects sharing the same definition.
 */
public class FixedObject {
    static final Object NO_VALUE = new Object();

    /**
     * The associated definition. Not final because we want to support the
     * ability to reassign the definition and the associated layout while
     * retaining the object identity.
     */
    private FixedObjectDefinition definition;
    /**
     * The layout which the object data currently complies with. May not
     * be the same as the definition's layout, indicating the need to
     * synchronize this object's data layout with the definition.
     */
    /*internal*/ volatile FixedObjectLayout layout;
    /*internal*/ Object[] referenceData;
    /*internal*/ int[] intData;

    public FixedObject(FixedObjectDefinition definition) {
        this.definition = definition;
    }

    public FixedObjectDefinition definition() {
        return definition;
    }

    FixedObjectLayout ensureUpToDateLayout() {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    // FIXME properly synchronize everything that must be synchronized

    public Object get(String fieldName) {
        ensureUpToDateLayout();
        var index = layout.fieldIndex(fieldName);
        if (index < 0) {
            throw RuntimeError.message("no such field: " + fieldName);
        }
        var ref = referenceData[index];
        return ref != NO_VALUE ? ref : intData[index];
    }

    public void set(String fieldName, Object value) {
        ensureUpToDateLayout();
        var index = layout.fieldIndex(fieldName);
        if (index < 0) {
            throw RuntimeError.message("no such field: " + fieldName);
        }
        if (value instanceof Integer) {
            referenceData[index] = NO_VALUE;
            intData[index] = (Integer) value;
        } else {
            referenceData[index] = value;
        }
    }
}
