// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.tmp;

import com.github.vassilibykov.trifle.builtin.Multiply;
import com.github.vassilibykov.trifle.core.Library;
import com.github.vassilibykov.trifle.core.UserFunction;

import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.bind;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.call;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.const_;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.direct;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.if_;
import static com.github.vassilibykov.trifle.expression.ExpressionLanguage.lambda;
import static com.github.vassilibykov.trifle.primitive.StandardPrimitiveLanguage.lessThan;
import static com.github.vassilibykov.trifle.primitive.StandardPrimitiveLanguage.sub;

public class BigFactorial {

    public static void main(String[] args) {
        var n = 200;
        var factorial = factorial();
        System.out.print("Warming up");
        for (int i = 0; i < 20; i++) {
            factorial.invoke(n);
            System.out.print(".");
        }
        System.out.println("done.");
        var start = System.nanoTime();
        var result = factorial.invoke(n);
        var elapsed = System.nanoTime() - start;
        System.out.format("factorial(%s) = %s in %s ms\n", n, result, elapsed / 1_000_000L);
    }

    private static UserFunction factorial() {
        Library toplevel = new Library();
        toplevel.define("factorial",
            factorial -> lambda(n ->
                if_(lessThan(n, const_(1)),
                    const_(1),
                    bind(call(direct(factorial), sub(n, const_(1))), t ->
                        call(direct(Multiply.INSTANCE), t, n)))));
        return toplevel.get("factorial");
    }
}
