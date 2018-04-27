// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * An executable representation of a {@link com.github.vassilibykov.trifle.expression.Call}
 * expression. This is an abstract superclass, with concrete implementations
 * for specific function arities.
 */
public abstract class CallNode extends EvaluatorNode {

    interface ArityMatcher<T> {
        T ifNullary();
        T ifUnary(EvaluatorNode arg);
        T ifBinary(EvaluatorNode arg1, EvaluatorNode arg2);
    }

    static CallNode with(CallDispatcher dispatcher, List<EvaluatorNode> args) {
        switch (args.size()) {
            case 0:
                return new Call0(dispatcher);
            case 1:
                return new Call1(dispatcher, args.get(0));
            case 2:
                return new Call2(dispatcher, args.get(0), args.get(1));
            default:
                throw new UnsupportedOperationException("arity > 2 not yet supported");
        }
    }

    /*
        Instance
     */

    @NotNull private CallDispatcher dispatcher;
    /*internal*/ final ValueProfile profile = new ValueProfile();

    CallNode(@NotNull CallDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    CallDispatcher dispatcher() {
        return dispatcher;
    }

    public abstract int arity();

    public abstract Stream<EvaluatorNode> arguments();

    public abstract EvaluatorNode argument(int index);

    public abstract <T> T match(ArityMatcher<T> matcher);

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall(this);
    }

    @Override
    public String toString() {
        return "call " + dispatcher;
    }

    /*
        Concrete implementations
     */

    private static class Call0 extends CallNode {
        private Call0(CallDispatcher dispatcher) {
            super(dispatcher);
        }

        @Override
        public int arity() {
            return 0;
        }

        @Override
        public Stream<EvaluatorNode> arguments() {
            return Stream.empty();
        }

        @Override
        public EvaluatorNode argument(int index) {
            throw new IllegalArgumentException();
        }

        @Override
        public <T> T match(ArityMatcher<T> matcher) {
            return matcher.ifNullary();
        }
    }

    private static class Call1 extends CallNode {
        @NotNull private final EvaluatorNode arg;

        private Call1(CallDispatcher dispatcher, @NotNull EvaluatorNode arg) {
            super(dispatcher);
            this.arg = arg;
        }

        public EvaluatorNode arg() {
            return arg;
        }

        @Override
        public int arity() {
            return 1;
        }

        @Override
        public Stream<EvaluatorNode> arguments() {
            return Stream.of(arg);
        }

        @Override
        public EvaluatorNode argument(int index) {
            if (index > 0) throw new IllegalArgumentException();
            return arg;
        }

        @Override
        public <T> T match(ArityMatcher<T> matcher) {
            return matcher.ifUnary(arg);
        }
    }

    private static class Call2 extends CallNode {
        @NotNull private final EvaluatorNode arg1;
        @NotNull private final EvaluatorNode arg2;

        private Call2(CallDispatcher dispatcher, @NotNull EvaluatorNode arg1, @NotNull EvaluatorNode arg2) {
            super(dispatcher);
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
        public int arity() {
            return 2;
        }

        @Override
        public Stream<EvaluatorNode> arguments() {
            return Stream.of(arg1, arg2);
        }

        @Override
        public EvaluatorNode argument(int index) {
            switch (index) {
                case 0: return arg1;
                case 1: return arg2;
                default: throw new IllegalArgumentException();
            }
        }

        @Override
        public <T> T match(ArityMatcher<T> matcher) {
            return matcher.ifBinary(arg1, arg2);
        }
    }
}
