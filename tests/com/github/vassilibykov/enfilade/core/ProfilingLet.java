// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfilingLet {
    protected UserFunction function;
    protected ValueProfile profile;

    @Before
    public void setUp() throws Exception {
        var topLevel = new Library();
        topLevel.define("test",
            lambda((shouldSet, setValue) ->
                bind(const_(1), t ->
                    if_(shouldSet,
                        set(t, setValue),
                        block()))));
        function = topLevel.get("test");
        var let = (LetNode) function.implementation().body();
        var tDef = let.variable();
        profile = tDef.profile;
    }

    @Test
    public void initialState() {
        assertTrue(profile.observedType().isUnknown());
    }

    @Test
    public void notSetting() {
        function.invoke(false, null);
        assertEquals(1, profile.intCases());
        assertEquals(0, profile.boolCases());
        assertEquals(0, profile.referenceCases());
        assertEquals(INT, profile.jvmType());
    }

    @Test
    public void settingInt() {
        function.invoke(true, 3);
        assertEquals(2, profile.intCases());
        assertEquals(0, profile.boolCases());
        assertEquals(0, profile.referenceCases());
        assertEquals(INT, profile.jvmType());
    }

    @Test
    public void settingReference() {
        function.invoke(true, "hello");
        function.invoke(true, "bye");
        assertEquals(2, profile.intCases());
        assertEquals(0, profile.boolCases());
        assertEquals(2, profile.referenceCases());
        assertEquals(REFERENCE, profile.jvmType());
    }
}
