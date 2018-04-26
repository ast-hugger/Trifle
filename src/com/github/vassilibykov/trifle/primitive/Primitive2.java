// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.expression.Primitive;

/**
 * The abstract superclass of binary primitive implementations.
 */
public abstract class Primitive2 implements Primitive {
    public abstract ExpressionType inferredType(ExpressionType argument1Type, ExpressionType argument2Type);
    public abstract Object apply(Object argument1, Object argument2);
    public abstract JvmType generate(GhostWriter writer, JvmType arg1Type, JvmType arg2Type);
}
