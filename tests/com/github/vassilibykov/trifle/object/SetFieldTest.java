// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.Library;
import com.github.vassilibykov.trifle.core.UserFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.assertEquals;

public class SetFieldTest {

    private Library topLevel;
    private UserFunction setFoo;
    private UserFunction setBar;
    private FixedObjectDefinition definition;
    private FixedObject instance;

    @Before
    public void setUp() throws Exception {
        topLevel = new Library();
        setFoo = topLevel.define("setFoo", lambda((instance, value) -> call(SetField.named("foo"), instance, value)));
        setBar = topLevel.define("setBar", lambda((instance, value) -> call(SetField.named("bar"), instance, value)));
        definition = new FixedObjectDefinition(List.of("foo", "bar"));
        instance = definition.instantiate();
    }

    @Test
    public void interpreted() {
        setFoo.invoke(instance, "hello there");
        setBar.invoke(instance, 7);
        assertEquals("hello there", instance.get("foo"));
        assertEquals(7, instance.get("bar"));
    }

    @Test
    public void interpretedNoProfiling() {
        setFoo.useSimpleInterpreter();
        setBar.useSimpleInterpreter();
        setFoo.invoke(instance, "hello there");
        setBar.invoke(instance, 7);
        assertEquals("hello there", instance.get("foo"));
        assertEquals(7, instance.get("bar"));
    }

    @Test
    public void compiled() {
        setFoo.forceCompile();
        setBar.forceCompile();
        setFoo.invoke(instance, "hello there");
        setBar.invoke(instance, 7);
        assertEquals("hello there", instance.get("foo"));
        assertEquals(7, instance.get("bar"));
    }

    @Test
    public void compiledWithInlineCache() {
        setFoo.forceCompile();
        setBar.forceCompile();
        setFoo.invoke(instance, "hello there");
        setBar.invoke(instance, 7);
        assertEquals("hello there", instance.get("foo"));
        assertEquals(7, instance.get("bar"));
        // second time through should be using the cached inline cache
        assertEquals("hello there", instance.get("foo"));
        assertEquals(7, instance.get("bar"));
    }
}