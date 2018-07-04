// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import com.github.vassilibykov.trifle.core.SquarePegException;

public abstract class Pair {

    public static Pair of(Object car, Object cdr) {
        return car instanceof Integer ? new PairI((Integer) car, cdr) : new PairL(car, cdr);
    }

    public static Pair of(int car, Object cdr) {
        return new PairI(car, cdr);
    }

    private static class PairL extends Pair {
        private final Object car;

        private PairL(Object car, Object cdr) {
            super(cdr);
            this.car = car;
        }

        @Override
        public Object car() {
            return car;
        }

        @Override
        public int intCar() {
            if (car instanceof Integer) {
                return (Integer) car;
            } else {
                throw SquarePegException.with(car);
            }
        }
    }

    private static class PairI extends Pair {
        private final int car;

        private PairI(int car, Object cdr) {
            super(cdr);
            this.car = car;
        }

        @Override
        public Object car() {
            return car;
        }

        @Override
        public int intCar() {
            return car;
        }
    }

    private final Object cdr;

    private Pair(Object cdr) {
        this.cdr = cdr;
    }

    public abstract Object car();
    public abstract int intCar();

    public Object cdr() {
        return cdr;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("(");
        printAsList(result);
        result.append(")");
        return result.toString();
    }

    private void printAsList(StringBuilder builder) {
        builder.append(car());
        if (cdr instanceof Pair) {
            builder.append(" ");
            ((Pair) cdr).printAsList(builder);
        } else if (cdr != null) {
            builder
                .append(" . ")
                .append(cdr());
        }
    }
}
