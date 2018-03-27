// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.tmp;

import com.github.vassilibykov.enfilade.core.FunctionTranslator;
import com.github.vassilibykov.enfilade.core.RuntimeFunction;
import com.github.vassilibykov.enfilade.expression.Function;
import com.github.vassilibykov.enfilade.expression.Variable;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.*;

public class TimeFib {

    public static void main(String[] args) {
        int n = 35;
        RuntimeFunction fibonacci = FunctionTranslator.translate(fibonacci());
        for (int i = 0; i < 40; i++) fibonacci.invoke(n);
        long start = System.nanoTime();
        int result = (Integer) fibonacci.invoke(n);
        long elapsed = System.nanoTime() - start;
        System.out.format("fibonacci(%s) = %s in %s ms\n", n, result, elapsed / 1_000_000L);
    }

    private static Function fibonacci() {
            Variable n = var("n");
            Variable t1 = var("t1");
            Variable t2 = var("t2");
            return com.github.vassilibykov.enfilade.expression.Function.recursive(n, fibonacci ->
                if_(lessThan(n, const_(2)),
                    const_(1),
                    let(t1, call(fibonacci, sub(n, const_(1))),
                        let(t2, call(fibonacci, sub(n, const_(2))),
                            add(t1, t2)))));
        }
}
