package com.github.vassilibykov.enfilade;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class Method {

    public static Method with(Var[] arguments, Expression body) {
        return new Method(arguments, body);
    }

    public static Method withRecursion(Var[] arguments, Function<Method, Expression> bodyMaker) {
        return new Method(arguments, bodyMaker);
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
                // reused as a let variable or a method argument.
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
                // as a method argument or a let binding.
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
    /*internal*/ final MethodProfile profile;

    private Method(@NotNull Var[] arguments, @NotNull Expression body) {
        this.arguments = arguments;
        this.body = body;
        this.localsCount = computeLocalsCount();
        this.profile = new MethodProfile(this);
    }

    private Method(@NotNull Var[] arguments, Function<Method, Expression> recursiveBodyMaker) {
        this.arguments = arguments;
        this.body = recursiveBodyMaker.apply(this);
        this.localsCount = computeLocalsCount();
        this.profile = new MethodProfile(this);
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
}
