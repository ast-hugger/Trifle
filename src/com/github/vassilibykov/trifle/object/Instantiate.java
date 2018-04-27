// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.CompilerError;
import com.github.vassilibykov.trifle.core.ExpressionType;
import com.github.vassilibykov.trifle.core.GhostWriter;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.RuntimeError;
import com.github.vassilibykov.trifle.primitive.Primitive1;

public class Instantiate extends Primitive1 {
    @Override
    public ExpressionType inferredType(ExpressionType argumentType) {
        return ExpressionType.known(JvmType.REFERENCE);
    }

    @Override
    public Object apply(Object argument) {
        if (!(argument instanceof FixedObjectDefinition)) {
            throw RuntimeError.message("not an object definition: " + argument);
        }
        return ((FixedObjectDefinition) argument).instantiate();
    }

    @Override
    public JvmType generate(GhostWriter writer, JvmType argType) {
        return argType.match(new JvmType.Matcher<>() {
            @Override
            public JvmType ifReference() {
                writer
                    .checkCast(FixedObjectDefinition.class)
                    .invokeVirtual(FixedObjectDefinition.class, "instantiateAsObject", Object.class);
                return JvmType.REFERENCE;
            }

            @Override
            public JvmType ifInt() {
                throw new CompilerError("INSTANTIATE is not applicable to an integer");
            }

            @Override
            public JvmType ifBoolean() {
                throw new CompilerError("INSTANTIATE is not applicable to a boolean");
            }
        });
    }
}
