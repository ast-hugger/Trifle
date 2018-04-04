// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.ExpressionType;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.JvmType;
import com.github.vassilibykov.enfilade.expression.Primitive;

/**
 * The abstract superclass of unary primitives implementations.
 */
public abstract class Primitive1 implements Primitive {
    public abstract ExpressionType inferredType(ExpressionType argumentType);
    public abstract Object apply(Object argument);
    public abstract JvmType generate(GhostWriter writer, JvmType argType);
}
