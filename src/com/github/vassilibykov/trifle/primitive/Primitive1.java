// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.expression.Primitive;

/**
 * The abstract superclass of unary primitives implementations.
 */
public abstract class Primitive1 implements Primitive {
    public abstract ExpressionType inferredType(ExpressionType argumentType);
    public abstract Object apply(Object argument);
    public abstract JvmType generate(GhostWriter writer, JvmType argType);
}
