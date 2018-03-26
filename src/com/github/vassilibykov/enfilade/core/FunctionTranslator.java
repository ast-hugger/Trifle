// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Block;
import com.github.vassilibykov.enfilade.expression.Call;
import com.github.vassilibykov.enfilade.expression.Const;
import com.github.vassilibykov.enfilade.expression.If;
import com.github.vassilibykov.enfilade.expression.Let;
import com.github.vassilibykov.enfilade.expression.PrimitiveCall;
import com.github.vassilibykov.enfilade.expression.Return;
import com.github.vassilibykov.enfilade.expression.SetVariable;
import com.github.vassilibykov.enfilade.expression.Variable;
import com.github.vassilibykov.enfilade.expression.Visitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates a function definition from the expression package into an
 * executable object.
 */
public class FunctionTranslator implements Visitor<EvaluatorNode> {

    public static RuntimeFunction translate(com.github.vassilibykov.enfilade.expression.Function function) {
        RuntimeFunction result = Environment.INSTANCE.lookupOrMake(function);
        FunctionTranslator translator = new FunctionTranslator(function);
        EvaluatorNode body = function.body().accept(translator);
        result.setArgumentsAndBody(translator.translatedArguments(), body);
        return result;
    }

    static class TranslationContext {
        private static TranslationContext simple(EvaluatorNode node) {
            return new TranslationContext(node, false);
        }

        private final EvaluatorNode result;
        private final boolean hasControlEscape;

        TranslationContext(EvaluatorNode result, boolean hasControlEscape) {
            this.result = result;
            this.hasControlEscape = hasControlEscape;
        }
    }

    /*
        Instance
     */

    private final com.github.vassilibykov.enfilade.expression.Function function;
    private final Map<Variable, VariableDefinition> variableDefinitions = new HashMap<>();

    private FunctionTranslator(com.github.vassilibykov.enfilade.expression.Function function) {
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
