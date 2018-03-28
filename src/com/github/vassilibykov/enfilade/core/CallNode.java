// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * An executable representation of a {@link com.github.vassilibykov.enfilade.expression.Call}
 * expression. This is an abstract superclass, with concrete implementations
 * for specific function arities.
 */
abstract class CallNode extends EvaluatorNode {
    @NotNull private EvaluatorNode function;
    /*internal*/ final ValueProfile profile = new ValueProfile();

    CallNode(@NotNull EvaluatorNode function) {
        this.function = function;
    }

    public EvaluatorNode function() {
        return function;
    }

    protected abstract int arity();

    @Override
    public String toString() {
        return "call " + function;
    }

    /*
        Concrete implementations
     */

    static class Call0 extends CallNode {

        Call0(EvaluatorNode function) {
            super(function);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitCall0(this);
        }

        @Override
        protected int arity() {
            return 0;
        }
    }

    static class Call1 extends CallNode {
        @NotNull private final EvaluatorNode arg;

        Call1(EvaluatorNode function, @NotNull EvaluatorNode arg) {
            super(function);
            this.arg = arg;
        }

        public EvaluatorNode arg() {
            return arg;
        }

        @Override
        protected int arity() {
            return 1;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitCall1(this);
        }
    }

    static class Call2 extends CallNode {
        @NotNull private final EvaluatorNode arg1;
        @NotNull private final EvaluatorNode arg2;

        Call2(EvaluatorNode function, @NotNull EvaluatorNode arg1, @NotNull EvaluatorNode arg2) {
            super(function);
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public EvaluatorNode arg1() {
            return arg1;
        }

        public EvaluatorNode arg2() {
            return arg2;
        }

        @Override
        protected int arity() {
            return 2;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitCall2(this);
        }
    }
}
