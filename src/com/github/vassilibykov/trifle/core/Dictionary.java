// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A mechanism for implementing a pool of non-method-local variables.
 * A program can get and set variable values using call expressions with
 * {@link com.github.vassilibykov.trifle.expression.DictionaryGetter}
 * and {@link com.github.vassilibykov.trifle.expression.DictionarySetter}
 * as callables.
 */
public class Dictionary {

    public static synchronized Dictionary create() {
        int id = REGISTRY.size();
        var dictionary = new Dictionary(id);
        REGISTRY.add(dictionary);
        return dictionary;
    }

    public static synchronized Dictionary withId(int id) {
        try {
            return REGISTRY.get(id);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("dictionary id not found: " + id);
        }
    }

    private static final List<Dictionary> REGISTRY = new ArrayList<>();
    private static final Object NO_VALUE = new Object();

    public static class Entry {
        private final String key;
        private int intValue;
        private Object refValue;

        private Entry(String key, Object value) {
            this.key = key;
            setValue(value);
        }

        public String key() {
            return key;
        }

        public Object value() {
            return refValue == NO_VALUE ? intValue : refValue;
        }

        public int intValue() {
            if (refValue == NO_VALUE) {
                return intValue;
            } else {
                throw SquarePegException.with(refValue);
            }
        }

        public void setValue(Object value) {
            if (value instanceof Integer) {
                this.intValue = (Integer) value;
                this.refValue = NO_VALUE;
            } else {
                this.refValue = value;
            }
        }

        public void setValue(int value) {
            this.intValue = value;
            this.refValue = NO_VALUE;
        }
    }

    /*
        Instance
     */

    private final int id;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private Dictionary(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public Map<String, Entry> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public Entry defineEntry(String key) {
        return entries.computeIfAbsent(key, k -> new Entry(k, null));
    }

    public Optional<Entry> getEntry(String key) {
        return Optional.ofNullable(entries.get(key));
    }
}
