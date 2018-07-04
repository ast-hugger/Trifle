// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class SmalltalkTest {
    @Test
    public void classicTest() {
        var smalltalk = Smalltalk.create();
        smalltalk.compileClass(
            "Object subclass: Test instanceVariables: ()" +
                "! test ^ 3 + 4");
        var testClass = smalltalk.findClass("Test");
        var testInstance = testClass.newInstance();
        assertEquals(7, testInstance.perform("test"));
    }
}