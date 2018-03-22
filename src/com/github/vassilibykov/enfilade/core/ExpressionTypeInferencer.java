// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.stream.Stream;

/**
 * Infers and sets the inferred types of compiler annotations in an expression.
 * The inferencing is of a simple bottom up kind.
 */
class ExpressionTypeInferencer implements Expression.Visitor<InferredType> {

    static void inferIn(Function function) {
        Stream.of(function.arguments()).forEach(
            each -> each.compilerAnnotation.setInferredType(InferredType.unknown()));
        function.body().accept(new ExpressionTypeInferencer());
    }

    private ExpressionTypeInferencer() {}

    @Override
    public InferredType visitCall0(Call0 call) {
        return andSetIn(call, InferredType.unknown());
    }

    @Override
    public InferredType visitCall1(Call1 call) {
        return andSetIn(call, InferredType.unknown());
    }

    @Override
    public InferredType visitCall2(Call2 call) {
        return andSetIn(call, InferredType.unknown());
    }

    @Override
    public InferredType visitConst(Const aConst) {
        return andSetIn(aConst, InferredType.known(TypeCategory.ofObject(aConst.value())));
    }

    @Override
    public InferredType visitIf(If anIf) {
        InferredType trueType = anIf.trueBranch().accept(this);
        InferredType falseType = anIf.falseBranch().accept(this);
        return andSetIn(anIf, trueType.union(falseType));
    }

    @Override
    public InferredType visitLet(Let let) {
        InferredType initType = let.initializer().accept(this);
        let.variable().compilerAnnotation.setInferredType(initType);
        return andSetIn(let, let.body().accept(this));
    }

    @Override
    public InferredType visitPrimitive1(Primitive1 primitive) {
        return andSetIn(primitive, InferredType.known(primitive.valueCategory()));
    }

    @Override
    public InferredType visitPrimitive2(Primitive2 primitive) {
        return andSetIn(primitive, InferredType.known(primitive.valueCategory()));
    }

    @Override
    public InferredType visitProg(Prog prog) {
        InferredType type = InferredType.known(TypeCategory.REFERENCE);
        for (Expression each : prog.expressions()) {
            type = each.accept(this);
        }
        return andSetIn(prog, type);
    }

    @Override
    public InferredType visitRet(Ret ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO need to think about this
    }

    @Override
    public InferredType visitSetVar(SetVar set) {
        InferredType valueType = set.value().accept(this);
        set.variable().compilerAnnotation.unifyInferredTypeWith(valueType);
        return andSetIn(set, valueType);
    }

    @Override
    public InferredType visitVar(Var var) {
        return var.compilerAnnotation.inferredType();
    }

    private InferredType andSetIn(Expression expression, InferredType type) {
        expression.compilerAnnotation.setInferredType(type);
        return type;
    }
}
