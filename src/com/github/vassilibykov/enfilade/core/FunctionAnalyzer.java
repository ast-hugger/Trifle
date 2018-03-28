// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs the post-translation analysis phase of {@link FunctionTranslator}.
 */
class FunctionAnalyzer {
    static void analyze(FunctionImplementation function) {
        var analyzer = new FunctionAnalyzer(function);
        analyzer.analyze();
    }

    /*
        Instance
     */
    @NotNull private final FunctionImplementation function;

    FunctionAnalyzer(@NotNull FunctionImplementation function) {
        this.function = function;
    }

    void analyze() {
        function.body().accept(new ScopeValidator());
        function.body().accept(new ClosureConverter(function));
        var variableIndexer = new VariableIndexer(function);
        variableIndexer.apply();
    }

    /**
     * Verifies that any variable reference is to a variable currently in scope.
     */
    private class ScopeValidator extends EvaluatorNode.VisitorSkeleton<Void> {
        private final Set<VariableDefinition> scope = new HashSet<>();

        private ScopeValidator() {
            scope.addAll(Arrays.asList(function.arguments()));
        }

        /**
         * The nested function is processed with the same validator because its inherits
         * the scope. However, its arguments and local variables are indexed anew.
         */
        @Override
        public Void visitClosure(ClosureNode closure) {
            var closureArgs = List.of(closure.function().arguments());
            for (var each : closureArgs) {
                if (scope.contains(each)) throw new CompilerError("closure argument is already bound: " + each);
            }
            scope.addAll(closureArgs);
            closure.function().body().accept(this);
            scope.removeAll(closureArgs);
            return null;
        }

        @Override
        public Void visitFreeVarReference(FreeVariableReferenceNode varRef) {
            return visitVarReference(varRef);
        }

        @Override
        public Void visitLet(LetNode let) {
            VariableDefinition var = let.variable();
            if (scope.contains(var)) throw new CompilerError("let variable is already bound: " + var);
            if (let.isLetrec()) {
                scope.add(var);
                let.initializer().accept(this);
            } else {
                let.initializer().accept(this);
                scope.add(var);
            }
            let.body().accept(this);
            scope.remove(var);
            return null;
        }

        @Override
        public Void visitSetFreeVar(SetFreeVariableNode set) {
            return visitSetVar(set);
        }

        @Override
        public Void visitSetVar(SetVariableNode set) {
            if (!scope.contains(set.variable)) {
                throw new CompilerError("referenced variable is not in scope: " + set.variable);
            }
            return null;
        }

        @Override
        public Void visitVarReference(VariableReferenceNode varRef) {
            if (!scope.contains(varRef.variable)) {
                throw new CompilerError("referenced variable is not in scope: " + varRef);
            }
            return null;
        }
    }

    /**
     * Analyzes closures nested in the top level function and plans their conversion
     * into functions with no free variables.
     */
    private class ClosureConverter extends EvaluatorNode.VisitorSkeleton<Void> {
        private final FunctionImplementation thisFunction;
        private final Set<VariableDefinition> freeVariables = new HashSet<>();

        ClosureConverter(FunctionImplementation thisFunction) {
            this.thisFunction = thisFunction;
        }

        public Set<VariableDefinition> freeVariables() {
            return freeVariables;
        }

        @Override
        public Void visitClosure(ClosureNode closure) {
            var nestedConverter = new ClosureConverter(closure.function());
            closure.function().body().accept(nestedConverter);
            closure.function().setFreeVariables(nestedConverter.freeVariables);
            nestedConverter.freeVariables.stream()
                .filter(some -> some.hostFunction() != thisFunction)
                .forEach(freeVariables::add);
            return null;
        }

        @Override
        public Void visitVarReference(VariableReferenceNode var) {
            var variable = var.variable;
            if (variable.hostFunction() != thisFunction) freeVariables.add(variable);
            return null;
        }
    }

    /**
     * Assigns generic indices to all let-bound variables in a function body. Indices are
     * allocated sequentially and uniquely. Two variables are guaranteed to have different
     * generic indices even if they are never live at the same time. This is necessary to
     * support closure escapes in interpreted code.
     */
    private class VariableIndexer extends EvaluatorNode.VisitorSkeleton<Void> {
        private final FunctionImplementation thisFunction;
        private int nextIndex;

        private VariableIndexer(FunctionImplementation function) {
            thisFunction = function;
            var arguments = function.arguments();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i].genericIndex = i;
                arguments[i].specializedIndex = i;
            }
            nextIndex = arguments.length;
        }

        public void apply() {
            thisFunction.body().accept(this);
            thisFunction.finishInitialization(nextIndex);
        }

        public int frameSize() {
            return nextIndex;
        }

        /**
         * The nested function is processed with the same validator because its inherits
         * the scope. However, its arguments and local variables are indexed anew.
         */
        @Override
        public Void visitClosure(ClosureNode closure) {
            new VariableIndexer(closure.function()).apply();
            return null;
        }

        @Override
        public Void visitLet(LetNode let) {
            if (let.isLetrec()) {
                let.variable().genericIndex = nextIndex++;
                let.initializer().accept(this);
            } else {
                let.initializer().accept(this);
                let.variable().genericIndex = nextIndex++;
            }
            let.body().accept(this);
            return null;
        }
    }
}
