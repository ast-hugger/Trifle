// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.stream.Stream;

/**
 * Analyzes an expression after it has been evaluated a number of times
 * by the {@link ProfilingInterpreter}. Summarizes the findings of values
 * observed during the evaluation in expression node annotations.
 */
class ExpressionTypeAnalyzer implements Expression.Visitor<TypeCategory> {

    static TypeCategory analyze(Function function) {
        Stream.of(function.arguments()).forEach(each -> each.accept(INSTANCE));
        return function.body().accept(INSTANCE);
    }

    private static final ExpressionTypeAnalyzer INSTANCE = new ExpressionTypeAnalyzer();

    private ExpressionTypeAnalyzer() {}

    @Override
    public TypeCategory visitCall0(Call0 call) {
        return annotate(call, call.profile.valueCategory());
    }

    @Override
    public TypeCategory visitCall1(Call1 call) {
        return annotate(call, call.profile.valueCategory());
    }

    @Override
    public TypeCategory visitCall2(Call2 call) {
        return annotate(call, call.profile.valueCategory());
    }

    @Override
    public TypeCategory visitConst(Const aConst) {
        return annotate(aConst, TypeCategory.ofObject(aConst.value()));
    }

    @Override
    public TypeCategory visitIf(If anIf) {
        return annotate(
            anIf,
            anIf.trueBranch().accept(this).union(anIf.falseBranch().accept(this)));
    }

    @Override
    public TypeCategory visitLet(Let let) {
        let.initializer().accept(this);
        let.variable().accept(this);
        return annotate(let.body(), let.body().accept(this));
    }

    @Override
    public TypeCategory visitPrimitive1(Primitive1 primitive1) {
        return annotate(primitive1, primitive1.valueCategory());
    }

    @Override
    public TypeCategory visitPrimitive2(Primitive2 primitive2) {
        return annotate(primitive2, primitive2.valueCategory());
    }

    @Override
    public TypeCategory visitProg(Prog prog) {
        Expression[] expressions = prog.expressions();
        if (expressions.length == 0) {
            return TypeCategory.REFERENCE;
        } else {
            Stream.of(expressions).forEach(each -> each.accept(this));
            return expressions[expressions.length - 1].compilerAnnotation.valueCategory();
        }
    }

    @Override
    public TypeCategory visitRet(Ret ret) {
        Expression expression = ret.value();
        return annotate(expression, expression.accept(this));
    }

    @Override
    public TypeCategory visitSetVar(SetVar set) {
        Expression expression = set.value();
        return annotate(expression, expression.accept(this));
    }

    @Override
    public TypeCategory visitVar(Var var) {
        return annotate(var, var.profile.valueCategory());
    }

    private TypeCategory annotate(Expression expression, TypeCategory category) {
        expression.setCompilerAnnotation(new CompilerAnnotation(category));
        return category;
    }
}
