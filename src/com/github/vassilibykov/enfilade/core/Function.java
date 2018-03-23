// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A function is the executable unit of code in Enfilade. Execution is launched
 * using the {@link #invoke} family of methods.
 */
public class Function {

    public static Function with(Variable[] arguments, Expression body) {
        return new Function(arguments, body);
    }

    public static Function withRecursion(Variable[] arguments, java.util.function.Function<Function, Expression> bodyBuilder) {
        return new Function(arguments, bodyBuilder);
    }

    /**
     * Assigns indices to all let-bound variables in a function body. Also
     * validates the use of variables, checking that any variable reference is
     * either to a function argument or to a let-bound variable currently in
     * scope.
     */
    private class VariableIndexer extends Expression.VisitorSkeleton<Void> {
        private int index;
        private final Set<Variable> scope = new HashSet<>();

        private VariableIndexer() {
            for (int i = 0; i < arguments.length; i++) {
                arguments[i].index = i;
            }
            this.index = arguments.length;
            scope.addAll(Arrays.asList(arguments));
        }

        public int index() {
            return index;
        }

        @Override
        public Void visitLet(Let let) {
            Variable var = let.variable();
            if (var.index() >= 0) {
                throw new AssertionError("variable reuse detected: " + var);
            }
            var.index = index++;
            let.initializer().accept(this);
            scope.add(var);
            let.body().accept(this);
            scope.remove(var);
            return null;
        }

        @Override
        public Void visitVarRef(VarRef varRef) {
            if (!scope.contains(varRef.variable)) {
                throw new AssertionError("variable used outside of its scope: " + varRef);
            }
            return null;
        }
    }

    /*
        Instance
     */

    @NotNull private final Variable[] arguments;
    @NotNull private final Expression body;
    private final int localsCount;
    /*internal*/ final Nexus nexus;
    /*internal*/ final FunctionProfile profile;

    private Function(@NotNull Variable[] arguments, @NotNull Expression body) {
        this.arguments = arguments;
        this.body = body;
        this.localsCount = computeLocalsCount();
        this.nexus = new Nexus(this);
        this.profile = new FunctionProfile(this);
    }

    private Function(@NotNull Variable[] arguments, java.util.function.Function<Function, Expression> recursiveBodyMaker) {
        this.arguments = arguments;
        this.body = recursiveBodyMaker.apply(this);
        this.localsCount = computeLocalsCount();
        this.nexus = new Nexus(this);
        this.profile = new FunctionProfile(this);
    }

    public Variable[] arguments() {
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
        VariableIndexer indexer = new VariableIndexer();
        body.accept(indexer);
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
