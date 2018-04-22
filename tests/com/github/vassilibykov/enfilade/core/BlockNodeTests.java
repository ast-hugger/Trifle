// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Before;
import org.junit.Test;

import java.lang.invoke.MethodType;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.block;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static org.junit.Assert.*;

@SuppressWarnings({"SimplifiableJUnitAssertion", "ConstantConditions"})
public class BlockNodeTests {

    private UserFunction emptyBlockClosure;
    private FunctionImplementation emptyBlockFunction;
    private BlockNode emptyBlockNode;
    private UserFunction blockClosure;
    private FunctionImplementation blockFunction;
    private BlockNode blockNode;

    @Before
    public void setUp() throws Exception {
        var topLevel = new Library();
        topLevel.define("emptyBlock",
            lambda(() -> block()));
        emptyBlockClosure = topLevel.get("emptyBlock");
        emptyBlockFunction = emptyBlockClosure.implementation();
        emptyBlockNode = (BlockNode) emptyBlockFunction.body();
        topLevel.define("block",
            lambda(() -> block(
                const_("one"),
                const_("two"),
                const_(3))));
        blockClosure = topLevel.get("block");
        blockFunction = blockClosure.implementation();
        blockNode = (BlockNode) blockFunction.body();
    }

    private void profileAndCompileAll() {
        emptyBlockClosure.invoke();
        blockClosure.invoke();
        emptyBlockFunction.forceCompile();
        blockFunction.forceCompile();
    }

    @Test
    public void profiledInterpretedEvaluation() {
        assertEquals(null, emptyBlockClosure.invoke());
        assertEquals(3, blockClosure.invoke());
    }

    @Test
    public void simpleInterpretedEvaluation() {
        emptyBlockFunction.useSimpleInterpreter();
        blockFunction.useSimpleInterpreter();
        assertEquals(null, emptyBlockClosure.invoke());
        assertEquals(3, blockClosure.invoke());
    }

    @Test
    public void inferredType() {
        profileAndCompileAll();
        assertEquals(REFERENCE, emptyBlockNode.inferredType().jvmType().get());
        assertEquals(INT, blockNode.inferredType().jvmType().get());
    }

    @Test
    public void specializedType() {
        profileAndCompileAll();
        assertEquals(REFERENCE, emptyBlockNode.specializedType());
        assertEquals(REFERENCE, emptyBlockFunction.specializedReturnType());
        assertEquals(INT, blockNode.specializedType());
        assertEquals(INT, blockFunction.specializedReturnType());
    }

    @Test
    public void compiledEvaluation() {
        profileAndCompileAll();
        assertEquals(MethodType.genericMethodType(0), emptyBlockFunction.genericImplementation().type());
        assertEquals(MethodType.genericMethodType(0), blockFunction.genericImplementation().type());
        assertEquals(null, emptyBlockClosure.invoke());
        assertEquals(3, blockClosure.invoke());
    }

    @Test
    public void noSpecializationPossible() {
        // because there are no parameters to make up a specialized signature
        assertEquals(null, emptyBlockFunction.specializedImplementation());
        assertEquals(null, blockFunction.specializedImplementation());
    }
}