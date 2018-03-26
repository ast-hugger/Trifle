// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.expression.AtomicExpression;
import com.github.vassilibykov.enfilade.expression.PrimitiveCall;

public class StandardPrimitiveLanguage {

    public static PrimitiveCall add(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(new PrimitiveKey(Add::new), arg1, arg2);
    }

    public static PrimitiveCall lessThan(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(new PrimitiveKey(LessThan::new), arg1, arg2);
    }

    public static PrimitiveCall mul(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(new PrimitiveKey(Mul::new), arg1, arg2);
    }

    public static PrimitiveCall sub(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(new PrimitiveKey(Sub::new), arg1, arg2);
    }

    public static PrimitiveCall negate(AtomicExpression arg) {
        return PrimitiveCall.with(new PrimitiveKey(Negate::new), arg);
    }
}
