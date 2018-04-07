// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValueProfileTest {
    private ValueProfile profile;

    @Before
    public void setUp() throws Exception {
        profile = new ValueProfile();
    }

    @Test
    public void initialState() {
        assertTrue(profile.observedType().isUnknown());
        assertEquals(REFERENCE, profile.jvmType());
        assertFalse(profile.hasProfileData());
    }

    @Test
    public void intCases() {
        profile.recordValue(1);
        profile.recordValue(2);
        profile.recordValue(3);
        assertTrue(profile.hasProfileData());
        assertFalse(profile.observedType().isUnknown());
        assertEquals(3, profile.intCases());
        assertEquals(0, profile.boolCases());
        assertEquals(0, profile.referenceCases());
        assertTrue(profile.isPureInt());
        assertFalse(profile.isPureBool());
        assertEquals(INT, profile.jvmType());
    }

    @Test
    public void boolCases() {
        profile.recordValue(true);
        profile.recordValue(false);
        assertFalse(profile.observedType().isUnknown());
        assertEquals(0, profile.intCases());
        assertEquals(2, profile.boolCases());
        assertEquals(0, profile.referenceCases());
        assertTrue(profile.isPureBool());
        assertFalse(profile.isPureInt());
        assertEquals(BOOL, profile.jvmType());
    }

    @Test
    public void referenceCases() {
        profile.recordValue("foo");
        profile.recordValue(new Object());
        assertFalse(profile.observedType().isUnknown());
        assertEquals(0, profile.intCases());
        assertEquals(0, profile.boolCases());
        assertEquals(2, profile.referenceCases());
        assertFalse(profile.isPureBool());
        assertFalse(profile.isPureInt());
        assertEquals(REFERENCE, profile.jvmType());
    }

    @Test
    public void mixedCases() {
        profile.recordValue(1);
        profile.recordValue(2);
        profile.recordValue(true);
        assertFalse(profile.observedType().isUnknown());
        assertEquals(2, profile.intCases());
        assertEquals(1, profile.boolCases());
        assertEquals(0, profile.referenceCases());
        assertFalse(profile.isPureInt());
        assertFalse(profile.isPureBool());
        assertEquals(REFERENCE, profile.jvmType());
    }
}
