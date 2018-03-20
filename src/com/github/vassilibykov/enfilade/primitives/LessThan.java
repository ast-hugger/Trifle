package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.AtomicExpression;
import com.github.vassilibykov.enfilade.core.GhostWriter;
import com.github.vassilibykov.enfilade.core.Primitive2;
import com.github.vassilibykov.enfilade.core.TypeCategory;
import org.jetbrains.annotations.NotNull;

public class LessThan extends Primitive2 {

    public LessThan(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        super(argument1, argument2);
    }

    @Override
    public TypeCategory valueCategory() {
        return TypeCategory.REFERENCE;
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 < (Integer) arg2;
    }

    @Override
    public void generate(GhostWriter writer) {
        writer.invokeStatic(LessThan.class, "staticApply", Object.class, Object.class, Object.class);
    }

    @SuppressWarnings("unused") // called by generated code
    public static Object staticApply(Object arg1, Object arg2) {
        return (Integer) arg1 < (Integer) arg2;
    }
}
