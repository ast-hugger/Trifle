// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade;

import com.github.vassilibykov.enfilade.core.FunctionTranslator;
import com.github.vassilibykov.enfilade.core.RunnableFunction;
import com.github.vassilibykov.enfilade.expression.Function;
import com.github.vassilibykov.enfilade.expression.Variable;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.*;
import static com.github.vassilibykov.enfilade.primitives.StandardPrimitiveLanguage.*;

public class TimeFib {

    /**
     * What exactly is profiled is determined by the {@link com.github.vassilibykov.enfilade.core.Nexus} class,
     * specifically the {@link com.github.vassilibykov.enfilade.core.Nexus#PROFILING_TARGET} (set to Integer.MAX_VALUE
     * to profile a pure interpreter), and by {@link com.github.vassilibykov.enfilade.core.Nexus#Nexus(RunnableFunction)}
     * (choose whether to use the profiling or the non-profiling interpreter).
     */
    public static void main(String[] args) {
        int n = 35;
        RunnableFunction fibonacci = FunctionTranslator.translate(fibonacci());
        for (int i = 0; i < 20; i++) fibonacci.invoke(n);
        long start = System.nanoTime();
        int result = (Integer) fibonacci.invoke(n);
        long elapsed = System.nanoTime() - start;
        System.out.format("fibonacci(%s) = %s in %s ms", n, result, elapsed / 1_000_000L);
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
