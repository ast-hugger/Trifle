// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.tmp;

import com.github.vassilibykov.enfilade.builtin.Add;
import com.github.vassilibykov.enfilade.builtin.Subtract;
import com.github.vassilibykov.enfilade.core.Library;
import com.github.vassilibykov.enfilade.core.UserFunction;

import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.direct;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.enfilade.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.enfilade.primitive.StandardPrimitiveLanguage.lessThan;

/**
 * Same as {@code TimeFib}, but using unlimited size integers.
 * The value of fib(35) we use for benchmark doesn't require an overflow
 * into big integers, but this is a benchmark which is more fair to
 * compare with Smalltalk.
 */
public class TimeBigFib {

    public static void main(String[] args) {
        var n = 35;
        var fibonacci = fibonacci();
        System.out.print("Warming up");
        for (int i = 0; i < 20; i++) {
            fibonacci.invoke(n);
            System.out.print(".");
        }
        System.out.println("done.");
        var start = System.nanoTime();
        var result = fibonacci.invoke(n);
        var elapsed = System.nanoTime() - start;
        System.out.format("fibonacci(%s) = %s in %s ms\n", n, result, elapsed / 1_000_000L);
    }

    private static UserFunction fibonacci() {
        Library toplevel = new Library();
        toplevel.define("fibonacci",
            fibonacci -> lambda(n ->
                if_(lessThan(n, const_(2)),
                    const_(1),
                    bind(call(direct(Subtract.INSTANCE), n, const_(1)), n1 ->
                        bind(call(direct(fibonacci), n1), t1 ->
                            bind(call(direct(Subtract.INSTANCE), n, const_(2)), n2 ->
                                bind(call(direct(fibonacci), n2), t2 ->
                                    call(direct(Add.INSTANCE), t1, t2))))))));
        return toplevel.get("fibonacci");
    }
}
