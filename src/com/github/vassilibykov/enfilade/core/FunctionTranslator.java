// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Block;
import com.github.vassilibykov.enfilade.expression.Call;
import com.github.vassilibykov.enfilade.expression.Const;
import com.github.vassilibykov.enfilade.expression.Lambda;
import com.github.vassilibykov.enfilade.expression.If;
import com.github.vassilibykov.enfilade.expression.Let;
import com.github.vassilibykov.enfilade.expression.LetRec;
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
 * Translates a pure function definition ({@link Lambda}) into an equivalent executable
 * {@link FunctionImplementation}. A translator is applied to a top-level lambda of function
 * definition. Any nested lambdas are processed by the same translator.
 *
 * <p>The following are translation stages performed by this class and by {@link FunctionAnalyzer}:
 * <ol>
 *     <li>For the top-level and all nested lambda expressions, a {@link FunctionImplementation}
 *     instance is created. {@link LambdaTranslator} in this class creates a tree of
 *     {@link EvaluatorNode}s equivalent to the original expressions. {@link VariableDefinition}s
 *     have their {@link VariableDefinition#isReferencedNonlocally} and
 *     {@link VariableDefinition#isMutable} properties set according to the structure of the source.</li>
 *     <li>Scope structure is validated by {@link FunctionAnalyzer.ScopeValidator} to ensure
 *     any variable references are in lexical scope of their variable definitions, and
 *     that variable definitions are not multiply bound in the original expression.</li>
 *     <li>Closure conversion is performed by {@link FunctionAnalyzer.ClosureConverter}, rewriting
 *     any free variable reference in a closure with a reference to a local synthetic parameter added
 *     to the closure. This step creates a number of {@link CopiedVariable} instances to represent
 *     the synthetic parameters, and replaces with them any references to {@link VariableDefinition}s
 *     from outer scopes.</li>
 *     <li>Variable indices are allocated to all variables of all functions, and frame sizes of all
 *     function are computed.</li>
 * </ol>
 *
 */
public class FunctionTranslator {

    public static Closure translate(Lambda lambda) {
        var implementation = FunctionRegistry.INSTANCE.lookupOrMake(lambda);
        var translator = new FunctionTranslator(lambda, implementation);
        translator.translate();
        FunctionAnalyzer.analyze(implementation); // finishesInitialization
        return new Closure(implementation, new Object[0]);
    }

    /*
        Instance
     */

    private final Lambda topLambda;
    private final FunctionImplementation functionImplementation;
    private final Map<Variable, VariableDefinition> variableDefinitions = new HashMap<>();
    private final Map<Lambda, FunctionImplementation> nestedFunctions = new HashMap<>();

    private FunctionTranslator(Lambda lambda, FunctionImplementation functionImplementation) {
        this.topLambda = lambda;
        this.functionImplementation = functionImplementation;
    }

    void translate() {
        var lambdaTranslator = new LambdaTranslator(topLambda, 0, functionImplementation);
        lambdaTranslator.translate();
    }

    private class LambdaTranslator implements Visitor<EvaluatorNode> {
        private final Lambda thisLambda;
        private final FunctionImplementation thisFunction;
        private List<VariableDefinition> arguments;

        private LambdaTranslator(Lambda thisLambda, int depth, FunctionImplementation thisFunction) {
            this.thisLambda = thisLambda;
            this.thisFunction = thisFunction;
            thisFunction.depth = depth;
        }

        void translate() {
            arguments = thisLambda.arguments().stream()
                .map(each -> variableDefinitions.computeIfAbsent(each,
                    definition -> new VariableDefinition(definition, thisFunction)))
                .collect(Collectors.toList());
            var body = thisLambda.body().accept(this);
            thisFunction.partiallyInitialize(arguments, body);
        }

        private VariableDefinition defineVariable(Variable variable) {
            if (variableDefinitions.containsKey(variable)) throw new CompilerError("variable is already defined");
            var definition = new VariableDefinition(variable, thisFunction);
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
            var expressions = block.expressions().stream()
                .map(each -> each.accept(this))
                .toArray(EvaluatorNode[]::new);
            return new BlockNode(expressions);
        }

        @Override
        public EvaluatorNode visitCall(Call call) {
            var target = call.target().accept(this);
            switch (call.arguments().size()) {
                case 0:
                    return new CallNode.Call0(target);
                case 1:
                    var arg = call.arguments().get(0).accept(this);
                    return new CallNode.Call1(target, arg);
                case 2:
                    var arg1 = call.arguments().get(0).accept(this);
                    var arg2 = call.arguments().get(1).accept(this);
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
        public EvaluatorNode visitLambda(Lambda lambda) {
            var nestedFunction = FunctionRegistry.INSTANCE.lookupOrMake(lambda);
            nestedFunctions.put(lambda, nestedFunction);
            var nestedTranslator = new LambdaTranslator(lambda, thisFunction.depth + 1, nestedFunction);
            nestedTranslator.translate();
            return new ClosureNode(lambda, nestedFunction);
        }

        @Override
        public EvaluatorNode visitLet(Let let) {
            var initializer = let.initializer().accept(this);
            var var = defineVariable(let.variable());
            return new LetNode(false, var, initializer,
                let.body().accept(this));
        }

        @Override
        public EvaluatorNode visitLetRec(LetRec letRec) {
            var var = defineVariable(letRec.variable());
            var.markAsMutable(); // even if it's not explicitly mutated!
            var initializer = letRec.initializer().accept(this);
            return new LetNode(true, var, initializer,
                letRec.body().accept(this));
        }

        @Override
        public EvaluatorNode visitPrimitiveCall(PrimitiveCall primitiveCall) {
            var args = primitiveCall.arguments().stream()
                .map(each -> each.accept(this))
                .collect(Collectors.toList());
            return primitiveCall.target().link(args);
        }

        @Override
        public EvaluatorNode visitReturn(Return aReturn) {
            return new ReturnNode(aReturn.value().accept(this));
        }

        @Override
        public EvaluatorNode visitSetVariable(SetVariable setVar) {
            var variable = lookupVariable(setVar.variable());
            variable.markAsMutable();
            if (variable.isFreeIn(thisFunction)) variable.markAsReferencedNonlocally();
            return new SetVariableNode(variable, setVar.value().accept(this));
        }

        @Override
        public EvaluatorNode visitVariable(Variable expressionVariable) {
            var variable = lookupVariable(expressionVariable);
            if (variable.isFreeIn(thisFunction)) variable.markAsReferencedNonlocally();
            return new GetVariableNode(variable);
        }
    }
}
