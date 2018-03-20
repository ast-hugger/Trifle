package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.AtomicExpression;
import com.github.vassilibykov.enfilade.GhostWriter;
import com.github.vassilibykov.enfilade.Primitive1;
import com.github.vassilibykov.enfilade.TypeCategory;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Negate extends Primitive1 {
    public Negate(@NotNull AtomicExpression argument) {
        super(argument);
    }

    @Override
    public TypeCategory valueCategory() {
        return TypeCategory.INT;
    }

    @Override
    public Object apply(Object arg) {
        return -((Integer) arg);
    }

    @Override
    public void generate(GhostWriter writer) {
        writer
            .checkCast(Integer.class)
            .invokeVirtual(Integer.class, "intValue", int.class)
            .loadInt(0)
            .swap();
        writer.methodWriter().visitInsn(Opcodes.ISUB);
        writer.invokeStatic(Integer.class, "valueOf", Integer.class, int.class);
    }
}
