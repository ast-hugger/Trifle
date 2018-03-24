// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.AtomicExpression;
import com.github.vassilibykov.enfilade.core.CompilerError;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.Primitive1;
import com.github.vassilibykov.enfilade.core.TypeCategory;
import org.jetbrains.annotations.NotNull;

import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;
import static org.objectweb.asm.Opcodes.ISUB;

public class Negate extends Primitive1 {
    public Negate(@NotNull AtomicExpression argument) {
        super(argument);
    }

    @Override
    public TypeCategory valueCategory() {
        return INT;
    }

    @Override
    public Object apply(Object arg) {
        return -((Integer) arg);
    }

    @Override
    public TypeCategory generate(GhostWriter writer, TypeCategory argCategory) {
        return argCategory.match(new TypeCategory.Matcher<TypeCategory>() {
            public TypeCategory ifReference() {
                writer
                    .adaptType(REFERENCE, INT)
                    .loadInt(0)
                    .swap()
                    .asm().visitInsn(ISUB);
                return INT;
            }
            public TypeCategory ifInt() {
                writer
                    .loadInt(0)
                    .swap()
                    .asm().visitInsn(ISUB);
                return INT;
            }
            public TypeCategory ifBoolean() {
                throw new CompilerError("NEGATE is not applicable to a boolean");
            }
        });
    }


    @Override
    public String toString() {
        return "(NEGATE " + argument() + ")";
    }
}
