// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import java.lang.invoke.SwitchPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable snapshot of the field-name-to-index mapping of a
 * {@link FixedObjectDefinition} at some point in time.
 */
class FixedObjectLayout {

    static class Migrator {
        private final int newSize;
        private final int[] oldToNewMap;

        private Migrator(FixedObjectLayout oldLayout, FixedObjectLayout newLayout) {
            this.newSize = newLayout.size();
            this.oldToNewMap = new int[oldLayout.size()];
            for (var entry : oldLayout.fieldIndices.entrySet()) {
                this.oldToNewMap[entry.getValue()] = newLayout.fieldIndex(entry.getKey());
            }
        }

        Object[] migrate(Object[] oldData) {
            Object[] newData = new Object[newSize];
            for (int i = 0; i < oldData.length; i++) {
                var newIndex = oldToNewMap[i];
                if (newIndex >= 0) newData[newIndex] = oldData[i];
            }
            return newData;
        }

        int[] migrate(int[] oldData) {
            int[] newData = new int[newSize];
            for (int i = 0; i < oldData.length; i++) {
                var newIndex = oldToNewMap[i];
                if (newIndex >= 0) newData[newIndex] = oldData[i];
            }
            return newData;
        }
    }

    /*
        Instance
     */

    /** Field names, in the order they appears in data arrays of objects with this layout. */
    private final List<String> fieldNames;
    /** A map from a field name to a field index for all field names. */
    private final Map<String, Integer> fieldIndices;
    /** Guards all dependent access sites; invalidated once this layout is no longer the current one. */
    private final SwitchPoint switchPoint;
    private final Map<FixedObjectLayout, Migrator> migrators = new HashMap<>();

    FixedObjectLayout(List<String> fieldNames) {
        this.fieldNames = Collections.unmodifiableList(new ArrayList<>(fieldNames));
        this.fieldIndices = new HashMap<>();
        for (int i = 0; i < this.fieldNames.size(); i++) {
            fieldIndices.put(this.fieldNames.get(i), i);
        }
        this.switchPoint = new SwitchPoint();
    }

    public int size() {
        return fieldNames.size();
    }

    public List<String> fieldNames() {
        return fieldNames;
    }

    public int fieldIndex(String name) {
        var index = fieldIndices.get(name);
        return index != null ? index : -1;
    }

    SwitchPoint switchPoint() {
        return switchPoint;
    }

    Migrator getMigrator(FixedObjectLayout target) {
        return migrators.computeIfAbsent(target, t -> new Migrator(this, t));
    }
}
