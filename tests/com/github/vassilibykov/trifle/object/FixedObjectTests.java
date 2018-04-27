// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

@SuppressWarnings("SimplifiableJUnitAssertion")
public class FixedObjectTests {

    private FixedObjectDefinition definition;
    private FixedObject instance;

    @Before
    public void setUp() throws Exception {
        definition = new FixedObjectDefinition(List.of("foo", "bar"));
        instance = definition.instantiate();
    }

    @Test
    public void initialState() {
        assertEquals(null, instance.get("foo"));
        assertEquals(null, instance.get("bar"));
    }

    @Test
    public void setAndGet() {
        instance.set("foo", 42);
        instance.set("bar", "hello");
        assertEquals(42, instance.get("foo"));
        assertEquals("hello", instance.get("bar"));
    }

    @Test
    public void definitionUpdate() {
        instance.set("foo", 42);
        instance.set("bar", "hello");
        definition.setFieldNames(List.of("foo", "zork", "bar"));
        instance.set("zork", "frobozz");
        assertEquals(42, instance.get("foo"));
        assertEquals("frobozz", instance.get("zork"));
        assertEquals("hello", instance.get("bar"));
    }
}