package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.AtomicExpression;
import com.github.vassilibykov.enfilade.Primitive2;
import org.jetbrains.annotations.NotNull;

public class Sub extends Primitive2 {

    public Sub(@NotNull AtomicExpression argument1, @NotNull AtomicExpression argument2) {
        super(argument1, argument2);
    }

    @Override
    public Object apply(Object arg1, Object arg2) {
        return (Integer) arg1 - (Integer) arg2;
    }

    @SuppressWarnings("unused") // called by generated code
    public static Object staticApply(Object arg1, Object arg2) {
        return (Integer) arg1 - (Integer) arg2;
    }
}
