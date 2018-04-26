// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.trifle.core.JvmType.INT;
import static com.github.vassilibykov.trifle.core.JvmType.REFERENCE;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test {@link CallNode}'s type profile which tracks the type of return values.
 */
public class ProfilingCall {
    private UserFunction function;
    private ValueProfile profile;

    @Before
    public void setUp() throws Exception {
        var topLevel = new Library();
        topLevel.define("callee",
            lambda(arg -> arg));
        topLevel.define("caller",
            lambda(arg -> call(topLevel.at("callee"), arg))); // the return value of the call is the arg
        function = topLevel.get("caller");
        var call = (CallNode) function.implementation().body();
        profile = call.profile;
    }

    @Test
    public void initialState() {
        assertTrue(profile.observedType().isUnknown());
    }

    @Test
    public void returningInt() {
        function.invoke(3);
        function.invoke(4);
        assertEquals(2, profile.intCases());
        assertEquals(0, profile.boolCases());
        assertEquals(0, profile.referenceCases());
        assertEquals(INT, profile.jvmType());
    }

    @Test
    public void returningReference() {
        function.invoke("hello");
        assertEquals(0, profile.intCases());
        assertEquals(0, profile.boolCases());
        assertEquals(1, profile.referenceCases());
        assertEquals(REFERENCE, profile.jvmType());
    }

    @Test
    public void returningMix() {
        function.invoke(true);
        function.invoke(1);
        assertEquals(1, profile.intCases());
        assertEquals(1, profile.boolCases());
        assertEquals(0, profile.referenceCases());
        assertEquals(REFERENCE, profile.jvmType());
    }
}
