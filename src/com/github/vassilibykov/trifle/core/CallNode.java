// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * An executable representation of a {@link com.github.vassilibykov.trifle.expression.Call}
 * expression. This is an abstract superclass, with concrete implementations
 * for specific function arities defined as nested private classes.
 */
public abstract class CallNode extends EvaluatorNode {

    /**
     * Code operating on {@link CallNode CallNodes} which needs to pattern-match
     * on the specific arity of a call can do so using this interface.
     */
    interface ArityMatcher<T> {
        T ifNullary();
        T ifUnary(EvaluatorNode arg);
        T ifBinary(EvaluatorNode arg1, EvaluatorNode arg2);
        T ifTernary(EvaluatorNode arg1, EvaluatorNode arg2, EvaluatorNode arg3);
        T ifQuaternary(EvaluatorNode arg1, EvaluatorNode arg2, EvaluatorNode arg3, EvaluatorNode arg4);
        T ifMultifarious(EvaluatorNode[] args);
    }

    static CallNode with(CallDispatcher dispatcher, List<EvaluatorNode> args) {
        switch (args.size()) {
            case 0:
                return new Arity0(dispatcher);
            case 1:
                return new Arity1(dispatcher, args.get(0));
            case 2:
                return new Arity2(dispatcher, args.get(0), args.get(1));
            case 3:
                return new Arity3(dispatcher, args.get(0), args.get(1), args.get(2));
            case 4:
                return new Arity4(dispatcher, args.get(0), args.get(1), args.get(2), args.get(3));
            default:
                return new ArityN(dispatcher, args.toArray(new EvaluatorNode[args.size()]));
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

    private static class Arity0 extends CallNode {
        private Arity0(CallDispatcher dispatcher) {
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

    private static class Arity1 extends CallNode {
        @NotNull private final EvaluatorNode arg;

        private Arity1(CallDispatcher dispatcher, @NotNull EvaluatorNode arg) {
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

    private static class Arity2 extends CallNode {
        @NotNull private final EvaluatorNode arg1;
        @NotNull private final EvaluatorNode arg2;

        private Arity2(CallDispatcher dispatcher, @NotNull EvaluatorNode arg1, @NotNull EvaluatorNode arg2) {
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

    private static class Arity3 extends CallNode {
        @NotNull private final EvaluatorNode arg1;
        @NotNull private final EvaluatorNode arg2;
        @NotNull private final EvaluatorNode arg3;

        private Arity3(@NotNull CallDispatcher dispatcher, @NotNull EvaluatorNode arg1, @NotNull EvaluatorNode arg2, @NotNull EvaluatorNode arg3) {
            super(dispatcher);
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
        }

        public EvaluatorNode arg1() {
            return arg1;
        }

        public EvaluatorNode arg2() {
            return arg2;
        }

        public EvaluatorNode arg3() {
            return arg3;
        }

        @Override
        public int arity() {
            return 3;
        }

        @Override
        public Stream<EvaluatorNode> arguments() {
            return Stream.of(arg1, arg2, arg3);
        }

        @Override
        public EvaluatorNode argument(int index) {
            switch (index) {
                case 0: return arg1;
                case 1: return arg2;
                case 2: return arg3;
                default: throw new IllegalArgumentException();
            }
        }

        @Override
        public <T> T match(ArityMatcher<T> matcher) {
            return matcher.ifTernary(arg1, arg2, arg3);
        }
    }

    private static class Arity4 extends CallNode {
        @NotNull private final EvaluatorNode arg1;
        @NotNull private final EvaluatorNode arg2;
        @NotNull private final EvaluatorNode arg3;
        @NotNull private final EvaluatorNode arg4;

        private Arity4(@NotNull CallDispatcher dispatcher, @NotNull EvaluatorNode arg1, @NotNull EvaluatorNode arg2,
                       @NotNull EvaluatorNode arg3, @NotNull EvaluatorNode arg4) {
            super(dispatcher);
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
        }

        public EvaluatorNode arg1() {
            return arg1;
        }

        public EvaluatorNode arg2() {
            return arg2;
        }

        public EvaluatorNode arg3() {
            return arg3;
        }

        public EvaluatorNode arg4() {
            return arg4;
        }

        @Override
        public int arity() {
            return 4;
        }

        @Override
        public Stream<EvaluatorNode> arguments() {
            return Stream.of(arg1, arg2, arg3, arg4);
        }

        @Override
        public EvaluatorNode argument(int index) {
            switch (index) {
                case 0: return arg1;
                case 1: return arg2;
                case 2: return arg3;
                case 3: return arg4;
                default: throw new IllegalArgumentException();
            }
        }

        @Override
        public <T> T match(ArityMatcher<T> matcher) {
            return matcher.ifQuaternary(arg1, arg2, arg3, arg4);
        }
    }

    private static class ArityN extends CallNode {
        @NotNull private final EvaluatorNode[] args;

        private ArityN(@NotNull CallDispatcher dispatcher, @NotNull EvaluatorNode[] args) {
            super(dispatcher);
            this.args = args;
        }

        public EvaluatorNode[] args() {
            return args;
        }

        @Override
        public int arity() {
            return args.length;
        }

        @Override
        public Stream<EvaluatorNode> arguments() {
            return Stream.of(args);
        }

        @Override
        public EvaluatorNode argument(int index) {
            return args[index];
        }

        @Override
        public <T> T match(ArityMatcher<T> matcher) {
            return matcher.ifMultifarious(args);
        }
    }
}
