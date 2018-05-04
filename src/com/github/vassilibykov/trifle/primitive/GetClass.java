// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;

public class GetClass extends Primitive1 {
    @Override
    public ExpressionType inferredType(ExpressionType argumentType) {
        return ExpressionType.known(JvmType.REFERENCE);
    }

    @Override
    public Object apply(Object argument) {
        return argument == null ? Void.class : argument.getClass();
    }

    @Override
    protected JvmType generateForReference(GhostWriter writer) {
        writer.invokeVirtual(Object.class, "getClass", Class.class, Object.class);
        return JvmType.REFERENCE;
    }

    @Override
    protected JvmType generateForInt(GhostWriter writer) {
        writer.asm().visitLdcInsn(Integer.class);
        return JvmType.REFERENCE;
    }

    @Override
    protected JvmType generateForBoolean(GhostWriter writer) {
        writer.asm().visitLdcInsn(Boolean.class);
        return JvmType.REFERENCE;
    }
}
