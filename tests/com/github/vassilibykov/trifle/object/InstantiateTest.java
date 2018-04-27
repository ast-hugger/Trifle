// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.Library;
import com.github.vassilibykov.trifle.core.UserFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.primitive;
import static org.junit.Assert.*;

public class InstantiateTest {

    private Library topLevel;
    private UserFunction make;
    private FixedObjectDefinition definition;

    @Before
    public void setUp() throws Exception {
        topLevel = new Library();
        make = topLevel.define("make",
            lambda(def ->
                bind(primitive(Instantiate.class, def), inst ->
                    block(
                        call(SetField.named("foo"), inst, const_("hello")),
                        call(SetField.named("bar"), inst, const_(42)),
                        inst))));
        definition = new FixedObjectDefinition(List.of("foo", "bar"));
    }

    @Test
    public void interpreted() {
        var instance = (FixedObject) make.invoke(definition);
        assertEquals("hello", instance.get("foo"));
        assertEquals(42, instance.get("bar"));
    }

    @Test
    public void interpretedNoProfiling() {
        make.useSimpleInterpreter();
        var instance = (FixedObject) make.invoke(definition);
        assertEquals("hello", instance.get("foo"));
        assertEquals(42, instance.get("bar"));
    }

    @Test
    public void compiled() {
        make.forceCompile();
        var instance = (FixedObject) make.invoke(definition);
        assertEquals("hello", instance.get("foo"));
        assertEquals(42, instance.get("bar"));
    }
}