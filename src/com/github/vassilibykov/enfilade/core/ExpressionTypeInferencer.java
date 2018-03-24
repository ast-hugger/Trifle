// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.stream.Stream;

/**
 * Infers and sets the inferred types of compiler annotations in an expression.
 * The inferencing is of a simple bottom up kind. A minor wrinkle is that if a
 * type originally assigned to a let-bound variable is widened as a result of
 * processing a {@code VarSet} expression, the entire expression tree needs to
 * be revisited as this may have changed the type of other expressions.
 *
 <p>An expression's inferred type, not to be confused with <em>observed
 * type</em> as recorded by the profiling interpreter, indicates what we know
 * about the value of the expression from static analysis of the expression
 * itself. For example, the inferred type of {@code (const 1)} is {@code int}
 * and the inferred type of {@code (const "foo")} is {@code reference}. Here, as
 * in many other places by type we mean the {@link TypeCategory} of a value, not
 * its type in the Java sense.
 * */
class ExpressionTypeInferencer implements Expression.Visitor<ExpressionType> {

    static void inferTypesIn(Function function) {
        Stream.of(function.arguments()).forEach(
            each -> each.compilerAnnotation.setInferredType(ExpressionType.unknown()));
        ExpressionTypeInferencer inferencer = new ExpressionTypeInferencer();
        do {
            inferencer.needsRevisiting = false;
            function.body().accept(inferencer);
            inferencer.firstVisit = false;
        } while (inferencer.needsRevisiting);
        // These iterative revisits are guaranteed to terminate because a revisit is
        // triggered by a type widening, and widening has an upper bound.
    }

    /*
        Instance
     */

    private boolean firstVisit = true;
    private boolean needsRevisiting = false;

    private ExpressionTypeInferencer() {}

    @Override
    public ExpressionType visitCall0(Call0 call) {
        return andSetIn(call, ExpressionType.unknown());
    }

    @Override
    public ExpressionType visitCall1(Call1 call) {
        call.arg().accept(this);
        return andSetIn(call, ExpressionType.unknown());
    }

    @Override
    public ExpressionType visitCall2(Call2 call) {
        call.arg1().accept(this);
        call.arg2().accept(this);
        return andSetIn(call, ExpressionType.unknown());
    }

    @Override
    public ExpressionType visitConst(Const aConst) {
        return andSetIn(aConst, ExpressionType.known(TypeCategory.ofObject(aConst.value())));
    }

    @Override
    public ExpressionType visitIf(If anIf) {
        ExpressionType testType = anIf.condition().accept(this);
        if (testType.typeCategory()
            .map(it -> !(it.equals(TypeCategory.BOOL) || it.equals(TypeCategory.REFERENCE)))
            .orElse(false))
        {
            throw new CompilerError("if() condition is not a boolean");
        }
        ExpressionType trueType = anIf.trueBranch().accept(this);
        ExpressionType falseType = anIf.falseBranch().accept(this);
        return andSetIn(anIf, trueType.union(falseType));
    }

    @Override
    public ExpressionType visitLet(Let let) {
        if (firstVisit) {
            ExpressionType initType = let.initializer().accept(this);
            let.variable().compilerAnnotation.setInferredType(initType);
        }
        return andSetIn(let, let.body().accept(this));
    }

    @Override
    public ExpressionType visitPrimitive1(Primitive1 primitive) {
        primitive.argument().accept(this);
        return andSetIn(primitive, ExpressionType.known(primitive.valueCategory()));
    }

    @Override
    public ExpressionType visitPrimitive2(Primitive2 primitive) {
        primitive.argument1().accept(this);
        primitive.argument2().accept(this);
        return andSetIn(primitive, ExpressionType.known(primitive.valueCategory()));
    }

    @Override
    public ExpressionType visitBlock(Block block) {
        ExpressionType type = ExpressionType.known(TypeCategory.REFERENCE);
        for (Expression each : block.expressions()) {
            type = each.accept(this);
        }
        return andSetIn(block, type);
    }

    @Override
    public ExpressionType visitRet(Ret ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO need to think about this
    }

    @Override
    public ExpressionType visitVarSet(VarSet set) {
        ExpressionType valueType = set.value().accept(this);
        if (set.variable().compilerAnnotation.unifyInferredTypeWith(valueType)) {
            needsRevisiting = true;
        }
        return andSetIn(set, valueType);
    }

    @Override
    public ExpressionType visitVarRef(VarRef varRef) {
        ExpressionType inferredType = varRef.variable.compilerAnnotation.inferredType();
        varRef.compilerAnnotation.setInferredType(inferredType);
        return inferredType;
    }

    private ExpressionType andSetIn(Expression expression, ExpressionType type) {
        expression.compilerAnnotation.setInferredType(type);
        return type;
    }
}
