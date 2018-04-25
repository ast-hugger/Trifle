// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.object;

/**
 * An object with storage locations (fields), identified by names (strings).
 * It is "fixed" in the sense that the set of fields of an object is determined
 * by its definition, and cannot be changed for a particular object without
 * changing it for all other objects sharing the same definition.
 */
public class FixedObject {
    private static final Object NO_VALUE = new Object();

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
    private FixedObjectLayout layout;
    private Object[] referenceData;
    private int[] intData;

    public FixedObject(FixedObjectDefinition definition) {
        this.definition = definition;
    }
}
