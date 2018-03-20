// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

public class Function {

    public static Function with(Var[] arguments, Expression body) {
        return new Function(arguments, body);
    }

    public static Function withRecursion(Var[] arguments, java.util.function.Function<Function, Expression> bodyMaker) {
        return new Function(arguments, bodyMaker);
    }

    private static class VariableIndexer extends Expression.VisitorSkeleton<Void> {
        private int index;

        private VariableIndexer(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        @Override
        public Void visitLet(Let let) {
            Var var = let.variable();
            if (var.index() >= 0) {
                // This happening is the sign of malformed expression, with the let variable
                // reused as a let variable or a function argument.
                throw new AssertionError("variable reuse detected: " + var);
            }
            var.setIndex(index++);
            return super.visitLet(let);
        }
    }

    private static class VariableIndexValidator extends Expression.VisitorSkeleton<Void> {
        @Override
        public Void visitVar(Var var) {
            if (var.index() < 0) {
                // This is an undeclared variable: used in an expression but not listed
                // as a function argument or a let binding.
                throw new AssertionError("undeclared variable: " + var);
            }
            return null;
        }
    }

    /*
        Instance
     */

    @NotNull private final Var[] arguments;
    @NotNull private final Expression body;
    private final int localsCount;
    /*internal*/ final Nexus nexus;
    /*internal*/ final FunctionProfile profile;

    private Function(@NotNull Var[] arguments, @NotNull Expression body) {
        this.arguments = arguments;
        this.body = body;
        this.localsCount = computeLocalsCount();
        this.nexus = new Nexus(this);
        this.profile = new FunctionProfile(this);
    }

    private Function(@NotNull Var[] arguments, java.util.function.Function<Function, Expression> recursiveBodyMaker) {
        this.arguments = arguments;
        this.body = recursiveBodyMaker.apply(this);
        this.localsCount = computeLocalsCount();
        this.nexus = new Nexus(this);
        this.profile = new FunctionProfile(this);
    }

    public Var[] arguments() {
        return arguments;
    }

    public Expression body() {
        return body;
    }

    public int arity() {
        return arguments.length;
    }

    public int localsCount() {
        return localsCount;
    }

    private int computeLocalsCount() {
        int i;
        for (i = 0; i < arguments.length; i++) {
            arguments[i].setIndex(i);
        }
        VariableIndexer indexer = new VariableIndexer(i);
        body.accept(indexer);
        body.accept(new VariableIndexValidator());
        return indexer.index();
    }

    public Object invoke() {
        return nexus.invoke();
    }

    public Object invoke(Object arg) {
        return nexus.invoke(arg);
    }

    public Object invoke(Object arg1, Object arg2) {
        return nexus.invoke(arg1, arg2);
    }
}
