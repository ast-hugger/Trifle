package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.AtomicExpression;
import com.github.vassilibykov.enfilade.Primitive1;
import org.jetbrains.annotations.NotNull;

public class Negate extends Primitive1 {
    public Negate(@NotNull AtomicExpression argument) {
        super(argument);
    }

    @Override
    public Object apply(Object arg) {
        return -((Integer) arg);
    }

    @SuppressWarnings("unused") // called by generated code
    public static Object staticApply(Object arg) {
        return -((Integer) arg);
    }
}
