// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.assertEquals;

/**
 * Test {@link IfNode}'s profiling, which tracks not the types but branch
 * evaluation counts.
 */
public class ProfilingIf {
    private Closure function;
    private IfNode ifNode;

    @Before
    public void setUp() throws Exception {
        var topLevel = new TopLevel();
        topLevel.define("test",
            lambda(arg ->
                if_(arg,
                    const_("hello"),
                    const_("bye"))));
        function = topLevel.getAsClosure("test");
        ifNode = (IfNode) function.implementation.body();
    }

    @Test
    public void initialState() {
        assertEquals(0, ifNode.trueBranchCount.intValue());
        assertEquals(0, ifNode.falseBranchCount.intValue());
    }

    @Test
    public void trueCases() {
        function.invoke(true);
        function.invoke(true);
        function.invoke(true);
        assertEquals(3, ifNode.trueBranchCount.intValue());
        assertEquals(0, ifNode.falseBranchCount.intValue());
    }

    @Test
    public void falseCases() {
        function.invoke(false);
        function.invoke(false);
        assertEquals(0, ifNode.trueBranchCount.intValue());
        assertEquals(2, ifNode.falseBranchCount.intValue());
    }

    @Test
    public void mixedCases() {
        function.invoke(true);
        function.invoke(true);
        function.invoke(false);
        assertEquals(2, ifNode.trueBranchCount.intValue());
        assertEquals(1, ifNode.falseBranchCount.intValue());
    }
}
