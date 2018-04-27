// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.Library;
import com.github.vassilibykov.trifle.core.UserFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.*;

public class GetFieldTest {

    private Library topLevel;
    private UserFunction getFoo;
    private UserFunction getBar;
    private FixedObjectDefinition definition;
    private FixedObject instance;

    @Before
    public void setUp() throws Exception {
        topLevel = new Library();
        getFoo = topLevel.define("getFoo", lambda(instance -> call(GetField.named("foo"), instance)));
        getBar = topLevel.define("getBar", lambda(instance -> call(GetField.named("bar"), instance)));
        definition = new FixedObjectDefinition(List.of("foo", "bar"));
        instance = definition.instantiate();
        instance.set("foo", "hello");
        instance.set("bar", 42);
    }

    @Test
    public void interpreted() {
        assertEquals("hello", getFoo.invoke(instance));
        assertEquals(42, getBar.invoke(instance));
    }

    @Test
    public void interpretedNoProfiling() {
        getFoo.useSimpleInterpreter();
        getBar.useSimpleInterpreter();
        assertEquals("hello", getFoo.invoke(instance));
        assertEquals(42, getBar.invoke(instance));
    }

    @Test
    public void compiled() {
        getFoo.forceCompile();
        getBar.forceCompile();
        assertEquals("hello", getFoo.invoke(instance));
        assertEquals(42, getBar.invoke(instance));
    }

    @Test
    public void compiledWithInlineCache() {
        getFoo.forceCompile();
        getBar.forceCompile();
        assertEquals("hello", getFoo.invoke(instance));
        assertEquals(42, getBar.invoke(instance));
        // second time through should be using the cached accessors
        assertEquals("hello", getFoo.invoke(instance));
        assertEquals(42, getBar.invoke(instance));
    }
}