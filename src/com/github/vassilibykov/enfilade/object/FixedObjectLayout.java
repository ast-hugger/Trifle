// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.object;

import java.lang.invoke.SwitchPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable snapshot of the field-name-to-index mapping of a
 * {@link FixedObjectDefinition} at some point in time.
 */
class FixedObjectLayout {
    /** Field names, in the order they appears in data arrays of objects with this layout. */
    private final List<String> fieldNames;
    /** Guards all dependent access sites; invalidated once this layout is no longer the current one. */
    private final SwitchPoint switchPoint;

    FixedObjectLayout(List<String> fieldNames) {
        this.fieldNames = Collections.unmodifiableList(new ArrayList<>(fieldNames));
        this.switchPoint = new SwitchPoint();
    }

    public List<String> fieldNames() {
        return fieldNames;
    }

    SwitchPoint switchPoint() {
        return switchPoint;
    }

    public int fieldIndex(String name) {
        return fieldNames.indexOf(name);
    }
}
