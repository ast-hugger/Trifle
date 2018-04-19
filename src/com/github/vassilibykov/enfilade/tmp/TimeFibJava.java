// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.tmp;

public class TimeFibJava {
    private static int fib(int n) {
        if (n < 2) {
            return 1;
        } else {
            return fib(n - 1) + fib(n - 2);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) {
            fib(35);
        }
        long start = System.currentTimeMillis();
        fib(35);
        long stop = System.currentTimeMillis();
        System.out.println(stop - start);
    }
}
