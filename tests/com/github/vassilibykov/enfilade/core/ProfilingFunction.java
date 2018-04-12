// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.TopLevel;
import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfilingFunction {
    private Closure function;
    private FunctionImplementation callee;
    private FunctionProfile calleeProfile;
    private ValueProfile calleeParamProfile;
    private FunctionImplementation caller;
    private FunctionProfile callerProfile;
    private ValueProfile callerParamProfile;

    @Before
    public void setUp() throws Exception {
        var topLevel = new TopLevel();
        topLevel.define("callee", lambda(arg -> arg));
        topLevel.define("caller", lambda(arg ->
            block(
                call(topLevel.at("callee"), arg),
                call(topLevel.at("callee"), const_("hello")))));
        function = topLevel.getAsClosure("caller");
        callee = topLevel.getAsClosure("callee").implementation;
        caller = topLevel.getAsClosure("caller").implementation;
        calleeProfile = callee.profile;
        calleeParamProfile = callee.declaredParameters().get(0).profile;
        callerProfile = caller.profile;
        callerParamProfile = caller.declaredParameters().get(0).profile;
    }

    @Test
    public void initialState() {
        assertTrue(calleeParamProfile.observedType().isUnknown());
        assertFalse(calleeParamProfile.hasProfileData());
        assertTrue(callerParamProfile.observedType().isUnknown());
        assertFalse(callerParamProfile.hasProfileData());
        assertEquals(0, calleeProfile.invocationCount());
        assertEquals(0, callerProfile.invocationCount());
    }

    @Test
    public void callWithInt() {
        function.invoke(3);
        function.invoke(4);
        caller.forceCompile();
        callee.forceCompile();
        assertEquals(2, callerProfile.invocationCount());
        assertEquals(4, calleeProfile.invocationCount());
        assertEquals(2, callerParamProfile.intCases());
        assertEquals(2, calleeParamProfile.intCases());
        assertEquals(2, calleeParamProfile.referenceCases());
        assertTrue(caller.canBeSpecialized());
        assertFalse(callee.canBeSpecialized());
    }

    @Test
    public void callWithReference() {
        function.invoke("hello");
        function.invoke("bye");
        caller.forceCompile();
        callee.forceCompile();
        assertEquals(0, callerParamProfile.intCases());
        assertEquals(2, callerParamProfile.referenceCases());
        assertEquals(4, calleeParamProfile.referenceCases());
        assertFalse(caller.canBeSpecialized());
        assertFalse(callee.canBeSpecialized());
    }
}
