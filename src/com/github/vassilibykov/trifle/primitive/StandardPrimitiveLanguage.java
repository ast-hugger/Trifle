// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.primitive;

import com.github.vassilibykov.trifle.expression.AtomicExpression;
import com.github.vassilibykov.trifle.expression.PrimitiveCall;

public class StandardPrimitiveLanguage {

    public static PrimitiveCall add(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(Add.class, arg1, arg2);
    }

    public static PrimitiveCall lessThan(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(LessThan.class, arg1, arg2);
    }

    public static PrimitiveCall mul(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(Mul.class, arg1, arg2);
    }

    public static PrimitiveCall sub(AtomicExpression arg1, AtomicExpression arg2) {
        return PrimitiveCall.with(Sub.class, arg1, arg2);
    }

    public static PrimitiveCall negate(AtomicExpression arg) {
        return PrimitiveCall.with(Negate.class, arg);
    }
}
