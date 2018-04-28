// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.junit.Before;
import org.junit.Test;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.set;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.while_;
import static com.github.vassilibykov.trifle.primitive.StandardPrimitiveLanguage.add;
import static com.github.vassilibykov.trifle.primitive.StandardPrimitiveLanguage.greaterThan;
import static com.github.vassilibykov.trifle.primitive.StandardPrimitiveLanguage.sub;
import static org.junit.Assert.*;

@SuppressWarnings({"ConstantConditions", "SimplifiableJUnitAssertion"})
public class WhileNodeTests {

    private Library topLevel;
    private UserFunction whileFalse;
    private UserFunction countdown;
    private UserFunction countdown2;
    private UserFunction adder;
    private FunctionImplementation implementation;
    private LetNode letNode;
    private BlockNode letBlock;
    private WhileNode whileNode;

    @Before
    public void setUp() throws Exception {
        topLevel = new Library();
        whileFalse = topLevel.define("whileFalse",
            lambda(() ->
                while_(const_(false),
                    const_("nothing"))));
        countdown = topLevel.define("countdown",
            lambda(arg ->
                while_(greaterThan(arg, const_(0)),
                    set(arg, sub(arg, const_(1))))));
        countdown2 = topLevel.define("countdown2",
            lambda((arg, sum) ->
                while_(greaterThan(arg, const_(0)),
                    set(sum, add(sum, const_(2))),
                    set(arg, sub(arg, const_(1))))));
        adder = topLevel.define("adder",
            lambda(arg ->
                bind(const_(0), sum ->
                    block(
                        while_(greaterThan(arg, const_(0)),
                            set(sum, add(sum, arg)),
                            set(arg, sub(arg, const_(1)))),
                        sum))));
        implementation = adder.implementation();
        letNode = (LetNode) implementation.body();
        letBlock = (BlockNode) letNode.body();
        whileNode = (WhileNode) letBlock.expressions()[0];
    }

    @Test
    public void whileFalseInterpreted() {
        assertEquals(null, whileFalse.invoke());
    }

    @Test
    public void whileFalseCompiled() {
        whileFalse.implementation().forceCompile();
        assertEquals(null, whileFalse.invoke());
    }

    @Test
    public void countdownInterpreted() {
        assertEquals(0, countdown.invoke(5));
    }

    @Test
    public void countdownCompiled() {
        countdown.implementation().forceCompile();
        assertEquals(0, countdown.invoke(5));
    }

    @Test
    public void countdown2Interpreted() {
        assertEquals(0, countdown2.invoke(5, 0));
    }

    @Test
    public void countdown2Compiled() {
        countdown2.implementation().forceCompile();
        assertEquals(0, countdown2.invoke(5, 0));
    }

    @Test
    public void profiledInterpretedEvaluation() {
        assertEquals(0, adder.invoke(0));
        assertEquals(1, adder.invoke(1));
        assertEquals(3, adder.invoke(2));
        assertEquals(6, adder.invoke(3));
        assertEquals(10, adder.invoke(4));
    }

    @Test
    public void simpleInterpretedEvaluation() {
        implementation.useSimpleInterpreter();
        assertEquals(0, adder.invoke(0));
        assertEquals(1, adder.invoke(1));
        assertEquals(3, adder.invoke(2));
        assertEquals(6, adder.invoke(3));
        assertEquals(10, adder.invoke(4));
    }

    @Test
    public void inferredType() {
        implementation.forceCompile();
        assertEquals(JvmType.INT, whileNode.inferredType().jvmType().get());
    }
}