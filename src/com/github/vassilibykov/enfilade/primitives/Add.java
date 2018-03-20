package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.AtomicExpression;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.Primitive2;
import com.github.vassilibykov.enfilade.core.TypeCategory;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

public class Add extends Primitive2 {

    public Add(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        super(argument1, argument2);
    }

    @Override
    public TypeCategory valueCategory() {
        return TypeCategory.INT;
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 + (Integer) arg2;
    }

    @Override
    public void generate(GhostWriter writer) {
        writer
            .checkCast(Integer.class)
            .invokeVirtual(Integer.class, "intValue", int.class)
            .swap()
            .checkCast(Integer.class)
            .invokeVirtual(Integer.class, "intValue", int.class)
            .swap();
        writer.methodWriter().visitInsn(Opcodes.IADD);
        writer.invokeStatic(Integer.class, "valueOf", Integer.class, int.class);
    }
}
