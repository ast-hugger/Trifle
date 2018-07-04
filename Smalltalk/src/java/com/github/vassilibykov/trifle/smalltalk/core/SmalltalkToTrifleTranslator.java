// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.expression.AtomicExpression;
import com.github.vassilibykov.trifle.expression.Call;
import com.github.vassilibykov.trifle.expression.Const;
import com.github.vassilibykov.trifle.expression.DictionaryGetter;
import com.github.vassilibykov.trifle.expression.DictionarySetter;
import com.github.vassilibykov.trifle.expression.Expression;
import com.github.vassilibykov.trifle.expression.Lambda;
import com.github.vassilibykov.trifle.expression.Let;
import com.github.vassilibykov.trifle.expression.SetVariable;
import com.github.vassilibykov.trifle.expression.Variable;
import com.github.vassilibykov.trifle.object.GetField;
import com.github.vassilibykov.trifle.object.SetField;
import com.github.vassilibykov.trifle.smalltalk.grammar.Assignment;
import com.github.vassilibykov.trifle.smalltalk.grammar.AstNode;
import com.github.vassilibykov.trifle.smalltalk.grammar.Block;
import com.github.vassilibykov.trifle.smalltalk.grammar.ClassDeclaration;
import com.github.vassilibykov.trifle.smalltalk.grammar.Literal;
import com.github.vassilibykov.trifle.smalltalk.grammar.MessageSend;
import com.github.vassilibykov.trifle.smalltalk.grammar.MethodDeclaration;
import com.github.vassilibykov.trifle.smalltalk.grammar.Return;
import com.github.vassilibykov.trifle.smalltalk.grammar.SourceUnit;
import com.github.vassilibykov.trifle.smalltalk.grammar.VarReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translates an AST of a Smalltalk method into a Trifle lambda,
 * normalizing the code on the fly.
 */
class SmalltalkToTrifleTranslator implements AstNode.Visitor<Expression> {

    static Lambda translate(MethodDeclaration method, Compiler.Scope scope) {
        var rewriter = new SmalltalkToTrifleTranslator(scope, true);
        return rewriter.translate(method);
    }

    /*
        Instance
     */

    private final Compiler.Scope scope;
    /** Indicates whether we are translating a method rather than a nested closure. */
    private final boolean isTopLevel;

    private SmalltalkToTrifleTranslator(Compiler.Scope scope, boolean isTopLevel) {
        this.scope = scope;
        this.isTopLevel = isTopLevel;
    }

    private Lambda translate(Block block) {
        var argNames = isTopLevel
            ? Stream.concat(Stream.of("self"), block.argumentNames().stream())
            : block.argumentNames().stream();
        var args = argNames
            .map(scope::lookupRequiredLocal)
            .collect(Collectors.toList());
        var temps = block.tempNames().stream()
            .map(scope::lookupRequiredLocal)
            .collect(Collectors.toList());
        var body = com.github.vassilibykov.trifle.expression.Block.with(
            block.expressions().stream()
                .map(each -> each.accept(this))
                .collect(Collectors.toList()));
        return Lambda.with(args, translateTemps(temps.iterator(), body));
    }

    private Expression translateTemps(Iterator<Variable> temps, Expression body) {
        if (!temps.hasNext()) return body;
        return Let.with(temps.next(), Const.value(null), translateTemps(temps, body));
    }

    @Override
    public Expression visitVarReference(VarReference varReference) {
        var binding = scope.lookup(varReference.name()).orElseThrow();
        return binding.match(new Binding.Matcher<>() {
            @Override
            public Expression ifGlobal(Binding.GlobalBinding globalBinding) {
                return Call.with(DictionaryGetter.of(globalBinding.entry()));
            }

            @Override
            public Expression ifInstVar(Binding.InstVarBinding instVarBinding) {
                return Call.with(GetField.named(instVarBinding.name()));
            }

            @Override
            public Expression ifLocal(Binding.LocalBinding localBinding) {
                return localBinding.variable();
            }
        });
    }

    @Override
    public Expression visitAssignment(Assignment assignment) {
        var binding = scope.lookup(assignment.variableName()).orElseThrow();
        var expr = assignment.expression().accept(this);
        return binding.match(new Binding.Matcher<>() {
            @Override
            public Expression ifGlobal(Binding.GlobalBinding globalBinding) {
                return normalize(expr, it -> Call.with(DictionarySetter.of(globalBinding.entry()), it));
            }

            @Override
            public Expression ifInstVar(Binding.InstVarBinding instVarBinding) {
                return normalize(expr, it -> Call.with(SetField.named(instVarBinding.name()), it));
            }

            @Override
            public Expression ifLocal(Binding.LocalBinding localBinding) {
                return SetVariable.with(localBinding.variable(), expr);
            }
        });
    }

    @Override
    public Expression visitBlock(Block block) {
        var nestedScope = scope.nestedScopeFor(block);
        return new SmalltalkToTrifleTranslator(nestedScope, false).translate(block);
    }

    @Override
    public Expression visitLiteral(Literal literal) {
        return Const.value(literal.value());
    }

    @Override
    public Expression visitMessageSend(MessageSend messageSend) {
        var arguments = new ArrayList<AtomicExpression>();
        var receiver = messageSend.receiver().accept(this);
        if (receiver instanceof AtomicExpression) {
            arguments.add((AtomicExpression) receiver);
            return translateSendArgs(
                messageSend.arguments().iterator(),
                arguments,
                args -> Call.with(
                    com.github.vassilibykov.trifle.object.MessageSend.selector(messageSend.selector()),
                    args));
        } else {
            var temp = gentemp();
            arguments.add(temp);
            return Let.with(temp, receiver, translateSendArgs(
                messageSend.arguments().iterator(),
                arguments,
                args -> Call.with(
                    com.github.vassilibykov.trifle.object.MessageSend.selector(messageSend.selector()),
                    args)));
        }
    }

    private Expression translateSendArgs(Iterator<AstNode> args, List<AtomicExpression> argExprs, Function<List<AtomicExpression>, Expression> finalizer) {
        if (!args.hasNext()) return finalizer.apply(argExprs);
        var expr = args.next().accept(this);
        if (expr instanceof AtomicExpression) {
            argExprs.add((AtomicExpression) expr);
            return translateSendArgs(args, argExprs, finalizer);
        } else {
            var temp = gentemp();
            argExprs.add(temp);
            return Let.with(temp, expr, translateSendArgs(args, argExprs, finalizer));
        }
    }

    /**
     * The wrinkle about returns is that a Smalltalk return is non-local
     * if contained in a closure nested in a method, returning from the
     * home method.
     */
    @Override
    public Expression visitReturn(Return aReturn) {
        var expr = aReturn.expression().accept(this);
        if (isTopLevel) {
            return normalize(expr, it -> com.github.vassilibykov.trifle.expression.Return.with(it));
        } else {
            throw new UnsupportedOperationException("not implemented yet"); // TODO implement
        }
    }

    /*
        The following are not supposed to be seen while visiting a method AST.
     */

    @Override
    public Expression visitSourceUnit(SourceUnit sourceUnit) {
        throw new UnsupportedOperationException("unexpected visit of " + sourceUnit);
    }

    @Override
    public Expression visitClassDeclaration(ClassDeclaration classDeclaration) {
        throw new UnsupportedOperationException("unexpected visit of " + classDeclaration);
    }

    @Override
    public Expression visitMethodDeclaration(MethodDeclaration methodDeclaration) {
        throw new UnsupportedOperationException("unexpected visit of " + methodDeclaration);
    }

    private int serial = 0;

    private Variable gentemp() {
        return Variable.named("$t" + serial++);
    }

    private Expression normalize(Expression subexpression, Function<AtomicExpression, Expression> generator) {
        if (subexpression instanceof AtomicExpression) {
            return generator.apply((AtomicExpression) subexpression);
        } else {
            var temp = gentemp();
            return Let.with(temp, subexpression, generator.apply(temp));
        }
    }
}
