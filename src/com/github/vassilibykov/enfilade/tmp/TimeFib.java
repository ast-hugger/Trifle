// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.tmp;

import com.github.vassilibykov.enfilade.core.Closure;
import com.github.vassilibykov.enfilade.core.FunctionTranslator;
import com.github.vassilibykov.enfilade.expression.Lambda;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.*;

public class TimeFib {

    public static void main(String[] args) {
        var n = 35;
        Closure fibonacci = FunctionTranslator.translate(fibonacci());
        System.out.print("Warming up");
        for (int i = 0; i < 10; i++) {
            fibonacci.invoke(n);
            System.out.print(".");
        }
        System.out.println("done.");
        var start = System.nanoTime();
        var result = (Integer) fibonacci.invoke(n);
        var elapsed = System.nanoTime() - start;
        System.out.format("fibonacci(%s) = %s in %s ms\n", n, result, elapsed / 1_000_000L);
    }

    private static Lambda fibonacci() {
        var fibonacci = var("fibonacci");
        var t1 = var("t1");
        var t2 = var("t2");
        return lambda(arg ->
            letrec(fibonacci, lambda(n ->
                    if_(lessThan(n, const_(2)),
                        const_(1),
                        let(t1, call(fibonacci, sub(n, const_(1))),
                            let(t2, call(fibonacci, sub(n, const_(2))),
                                add(t1, t2))))),
                call(fibonacci, arg)));
    }
}
