// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Block;
import com.github.vassilibykov.enfilade.expression.Call;
import com.github.vassilibykov.enfilade.expression.Const;
import com.github.vassilibykov.enfilade.expression.Function;
import com.github.vassilibykov.enfilade.expression.If;
import com.github.vassilibykov.enfilade.expression.Let;
import com.github.vassilibykov.enfilade.expression.PrimitiveCall;
import com.github.vassilibykov.enfilade.expression.Return;
import com.github.vassilibykov.enfilade.expression.SetVariable;
import com.github.vassilibykov.enfilade.expression.Variable;
import com.github.vassilibykov.enfilade.expression.Visitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Translates a pure function definition ({@link Function})
 * into an equivalent executable {@link RuntimeFunction}.
 */
public class FunctionTranslator implements Visitor<EvaluatorNode> {

    public static RuntimeFunction translate(Function function) {
        RuntimeFunction runtimeFunction = Environment.INSTANCE.lookupOrMake(function);
        FunctionTranslator translator = new FunctionTranslator(function);
        EvaluatorNode body = function.body().accept(translator);
        VariableIndexer indexer = new VariableIndexer(translator.translatedArguments());
        body.accept(indexer);
        runtimeFunction.finishInitialization(translator.translatedArguments(), body, indexer.maxIndex());
        return runtimeFunction;
    }

    /**
     * Assigns generic indices to all let-bound variables in a function body.
     * Also validates the use of variables, checking that any variable reference
     * is either to a function argument or to a let-bound variable currently in
     * scope.
     */
    private static class VariableIndexer extends EvaluatorNode.VisitorSkeleton<Void> {
        private int currentIndex;
        private int maxIndex;
        private final Set<VariableDefinition> scope = new HashSet<>();

        private VariableIndexer(VariableDefinition[] arguments) {
            for (int i = 0; i < arguments.length; i++) {
                arguments[i].genericIndex = i;
            }
            currentIndex = arguments.length;
            maxIndex = currentIndex;
            scope.addAll(Arrays.asList(arguments));
        }

        public int maxIndex() {
            return maxIndex;
        }

        @Override
        public Void visitLet(LetNode let) {
            VariableDefinition var = let.variable();
            if (var.genericIndex() >= 0) {
                throw new AssertionError("variable reuse detected: " + var);
            }
            let.initializer().accept(this);
            var.genericIndex = currentIndex++;
            maxIndex = Math.max(maxIndex, currentIndex);
            scope.add(var);
            let.body().accept(this);
            scope.remove(var);
            currentIndex--;
            return null;
        }

        @Override
        public Void visitVarRef(VariableReferenceNode varRef) {
            if (!scope.contains(varRef.variable)) {
                throw new AssertionError("variable used outside of its scope: " + varRef);
            }
            return null;
        }
    }

    /*
        Instance
     */

    private final Function function;
    private final Map<Variable, VariableDefinition> variableDefinitions = new HashMap<>();

    private FunctionTranslator(Function function) {
        this.function = function;
        translatedArguments();
    }

    private VariableDefinition[] translatedArguments() {
        return function.arguments().stream()
            .map(each -> variableDefinitions.computeIfAbsent(each, VariableDefinition::new))
            .toArray(VariableDefinition[]::new);
    }

    private VariableDefinition defineVariable(Variable variable) {
        if (variableDefinitions.containsKey(variable)) throw new CompilerError("variable is already defined");
        VariableDefinition definition = new VariableDefinition(variable);
        variableDefinitions.put(variable, definition);
        return definition;
    }

    private VariableDefinition lookupVariable(Variable variable) {
        return variableDefinitions.computeIfAbsent(variable, k -> {
            throw new CompilerError("variable has not been defined: " + variable);
        });
    }

    @Override
    public EvaluatorNode visitBlock(Block block) {
        EvaluatorNode[] expressions = block.expressions().stream()
            .map(each -> each.accept(this))
            .toArray(EvaluatorNode[]::new);
        return new BlockNode(expressions);
    }

    @Override
    public EvaluatorNode visitCall(Call call) {
        RuntimeFunction target = Environment.INSTANCE.lookupOrMake(call.target());
        switch (call.arguments().size()) {
            case 0:
                return new CallNode.Call0(target);
            case 1:
                EvaluatorNode arg = call.arguments().get(0).accept(this);
                return new CallNode.Call1(target, arg);
            case 2:
                EvaluatorNode arg1 = call.arguments().get(0).accept(this);
                EvaluatorNode arg2 = call.arguments().get(1).accept(this);
                return new CallNode.Call2(target, arg1, arg2);
            default:
                throw new UnsupportedOperationException("not yet implemented");
        }
    }

    @Override
    public EvaluatorNode visitConst(Const aConst) {
        return new ConstNode(aConst.value());
    }

    @Override
    public EvaluatorNode visitIf(If anIf) {
        return new IfNode(
            anIf.condition().accept(this),
            anIf.trueBranch().accept(this),
            anIf.falseBranch().accept(this));
    }

    @Override
    public EvaluatorNode visitLet(Let let) {
        EvaluatorNode initializer = let.initializer().accept(this);
        VariableDefinition var = defineVariable(let.variable());
        return new LetNode(
            var,
            initializer,
            let.body().accept(this));
    }

    @Override
    public EvaluatorNode visitPrimitiveCall(PrimitiveCall primitiveCall) {
        List<EvaluatorNode> args = primitiveCall.arguments().stream()
            .map(each -> each.accept(this))
            .collect(Collectors.toList());
        return primitiveCall.target().link(args);
    }

    @Override
    public EvaluatorNode visitReturn(Return aReturn) {
        return new ReturnNode(aReturn.value().accept(this));
    }

    @Override
    public EvaluatorNode visitSetVariable(SetVariable setVariable) {
        return new SetVariableNode(lookupVariable(setVariable.variable()), setVariable.value().accept(this));
    }

    @Override
    public EvaluatorNode visitVariable(Variable variable) {
        return new VariableReferenceNode(lookupVariable(variable));
    }
}
