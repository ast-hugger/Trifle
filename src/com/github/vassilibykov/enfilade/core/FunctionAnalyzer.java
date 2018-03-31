// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        new ScopeValidator().apply();
        new ClosureConverter(function).apply();
        new VariableIndexer(function).apply();
    }

    /**
     * Verifies that any variable reference is to a variable currently in scope.
     */
    private class ScopeValidator extends EvaluatorNode.VisitorSkeleton<Void> {
        private final Set<AbstractVariable> scope = new HashSet<>();

        private ScopeValidator() {
            scope.addAll(function.declaredParameters());
        }

        void apply() {
            function.body().accept(this);
        }

        /**
         * The nested function is processed with the same validator because its inherits
         * the scope. However, its arguments and local variables are indexed anew.
         */
        @Override
        public Void visitClosure(ClosureNode closure) {
            var closureArgs = closure.function().declaredParameters();
            for (var each : closureArgs) {
                if (scope.contains(each)) throw new CompilerError("closure argument is already bound: " + each);
            }
            scope.addAll(closureArgs);
            closure.function().body().accept(this);
            scope.removeAll(closureArgs);
            return null;
        }

        @Override
        public Void visitGetVar(GetVariableNode varRef) {
            if (!scope.contains(varRef.variable())) {
                throw new CompilerError("referenced variable is not in scope: " + varRef.variable());
            }
            return null;
        }

        @Override
        public Void visitLet(LetNode let) {
            AbstractVariable var = let.variable();
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
        public Void visitSetVar(SetVariableNode set) {
            if (!scope.contains(set.variable())) {
                throw new CompilerError("referenced variable is not in scope: " + set.variable());
            }
            return null;
        }
    }

    /**
     * Analyzes closures nested in the top level function, introduces synthetic arguments
     * to copy down values of free variable references, and rewrites free variable
     * references to point at the copied down locally available values.
     */
    private class ClosureConverter extends EvaluatorNode.VisitorSkeleton<Void> {
        private final FunctionImplementation thisFunction;
        /**
         * At the end of a visit contains all free variables of this function
         * and of functions nested in it (transitively).
         */
        private final Set<AbstractVariable> freeVariables = new HashSet<>();
        private final Map<VariableDefinition, CopiedVariable> rewrittenVariables = new HashMap<>();

        ClosureConverter(FunctionImplementation thisFunction) {
            this.thisFunction = thisFunction;
        }

        void apply() {
            thisFunction.body().accept(this);
            thisFunction.setSyntheticParameters(rewrittenVariables.values());
        }

        @Override
        public Void visitClosure(ClosureNode closure) {
            var nestedConverter = new ClosureConverter(closure.function());
            nestedConverter.apply();
            // Now 'closure.function()' has its 'syntheticParameters' set; they should be processed as
            // any other variable reference in this function.
            for (var each : closure.function().syntheticParameters()) {
                if (each.original().hostFunction() == thisFunction) {
                    each.setSupplier(each.original());
                } else {
                    var copiedVariable = rewriteFreeVariable(each.original());
                    each.setSupplier(copiedVariable);
                }
            }
            return null;
        }

        @Override
        public Void visitGetVar(GetVariableNode getVar) {
            var variable = getVar.variable();
            if (variable.hostFunction() != thisFunction) {
                var copiedVariable = rewriteFreeVariable(variable);
                getVar.replaceVariable(copiedVariable);
            }
            return null;
        }

        @Override
        public Void visitSetVar(SetVariableNode setVar) {
            var variable = setVar.variable();
            if (variable.hostFunction() != thisFunction) {
                var copiedVariable = rewriteFreeVariable(variable);
                setVar.replaceVariable(copiedVariable);
            }
            return null;
        }

        private CopiedVariable rewriteFreeVariable(AbstractVariable var) {
            var definition = (VariableDefinition) var; // cast must succeed for any free variable
            return rewrittenVariables.computeIfAbsent(definition, k -> new CopiedVariable(k, thisFunction));
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
            var arguments = function.allParameters();
            int i;
            for (i = 0; i < arguments.length; i++) {
                arguments[i].genericIndex = i;
                arguments[i].specializedIndex = i;
            }
            nextIndex = i;
        }

        public void apply() {
            thisFunction.body().accept(this);
            thisFunction.finishInitialization(nextIndex);
        }

        /**
         * The nested function is processed with the same validator because its inherits
         * the scope. However, its arguments and local variables are indexed anew.
         */
        @Override
        public Void visitClosure(ClosureNode closure) {
            var nestedIndexer = new VariableIndexer(closure.function());
            nestedIndexer.apply();
            closure.copiedOuterVariables = closure.function().syntheticParameters().stream()
                .map(each -> each.supplier())
                .collect(Collectors.toList());
            closure.copiedVariablesGenericIndices = closure.copiedOuterVariables.stream()
                .mapToInt(each -> each.genericIndex())
                .toArray();
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
