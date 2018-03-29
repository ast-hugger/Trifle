// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.core.JvmType.VOID;

/**
 * Infers and sets the inferred types of compiler annotations in an expression.
 * The inferencing is of a simple bottom up kind. A minor wrinkle is that if a
 * type originally assigned to a let-bound variable is widened as a result of
 * processing a {@code SetVariableNode} expression, the entire expression tree needs to
 * be revisited as this may have changed the type of other expressions.
 *
 <p>An expression's inferred type, not to be confused with <em>observed
 * type</em> as recorded by the profiling interpreter, indicates what we know
 * about the value of the expression from static analysis of the expression
 * itself. For example, the inferred type of {@code (const 1)} is {@code int}
 * and the inferred type of {@code (const "foo")} is {@code reference}. Here, as
 * in many other places by type we mean the {@link JvmType} of a value, not
 * its type in the Java sense.
 * */
class ExpressionTypeInferencer implements EvaluatorNode.Visitor<ExpressionType> {

    static void inferTypesIn(FunctionImplementation function) {
        function.parameters().forEach(
            each -> each.setInferredType(ExpressionType.unknown()));
        ExpressionTypeInferencer inferencer = new ExpressionTypeInferencer(function);
        do {
            inferencer.needsRevisiting = false;
            function.body().accept(inferencer);
        } while (inferencer.needsRevisiting);
        // These iterative revisits are guaranteed to terminate because a revisit is
        // triggered by a type widening, and widening has an upper bound.
    }

    /*
        Instance
     */

    private final EvaluatorNode functionBody;
    private boolean needsRevisiting = false;

    private ExpressionTypeInferencer(FunctionImplementation function) {
        this.functionBody = function.body();
    }

    @Override
    public ExpressionType visitCall0(CallNode.Call0 call) {
        return andSetIn(call, ExpressionType.unknown());
    }

    @Override
    public ExpressionType visitCall1(CallNode.Call1 call) {
        call.arg().accept(this);
        return andSetIn(call, ExpressionType.unknown());
    }

    @Override
    public ExpressionType visitCall2(CallNode.Call2 call) {
        call.arg1().accept(this);
        call.arg2().accept(this);
        return andSetIn(call, ExpressionType.unknown());
    }

    @Override
    public ExpressionType visitClosure(ClosureNode closure) {
        return andSetIn(closure, ExpressionType.known(REFERENCE));
    }

    @Override
    public ExpressionType visitConst(ConstNode aConst) {
        return andSetIn(aConst, ExpressionType.known(JvmType.ofObject(aConst.value())));
    }

    @Override
    public ExpressionType visitIf(IfNode anIf) {
        ExpressionType testType = anIf.condition().accept(this);
        if (testType.jvmType()
            .map(it -> !(it.equals(JvmType.BOOL) || it.equals(JvmType.REFERENCE)))
            .orElse(false))
        {
            throw new CompilerError("if() condition is not a boolean");
        }
        ExpressionType trueType = anIf.trueBranch().accept(this);
        ExpressionType falseType = anIf.falseBranch().accept(this);
        return andSetIn(anIf, trueType.union(falseType));
    }

    @Override
    public ExpressionType visitLet(LetNode let) {
        ExpressionType initType = let.initializer().accept(this);
        let.variable().unifyInferredTypeWith(initType);
        return andSetIn(let, let.body().accept(this));
    }

    @Override
    public ExpressionType visitPrimitive1(Primitive1Node primitive) {
        primitive.argument().accept(this);
        return andSetIn(primitive, ExpressionType.known(primitive.jvmType()));
    }

    @Override
    public ExpressionType visitPrimitive2(Primitive2Node primitive) {
        primitive.argument1().accept(this);
        primitive.argument2().accept(this);
        return andSetIn(primitive, ExpressionType.known(primitive.jvmType()));
    }

    @Override
    public ExpressionType visitBlock(BlockNode block) {
        ExpressionType type = ExpressionType.known(JvmType.REFERENCE);
        for (EvaluatorNode each : block.expressions()) {
            type = each.accept(this);
        }
        return andSetIn(block, type);
    }

    /**
     * A ReturnNode is unusual compared to others in that its own type is void
     * because its continuation never receives any value, but the type
     * of the returned value must be incorporated into the type of the
     * function body.
     */
    @Override
    public ExpressionType visitRet(ReturnNode ret) {
        ExpressionType valueType = ret.value().accept(this);
        functionBody.unifyInferredTypeWith(valueType);
        return andSetIn(ret, ExpressionType.known(VOID));
    }

    @Override
    public ExpressionType visitSetVar(SetVariableNode set) {
        ExpressionType valueType = set.value().accept(this);
        if (set.variable().unifyInferredTypeWith(valueType)) {
            needsRevisiting = true;
        }
        return andSetIn(set, valueType);
    }

    @Override
    public ExpressionType visitGetVar(GetVariableNode varRef) {
        ExpressionType inferredType = varRef.variable().inferredType();
        if (varRef.unifyInferredTypeWith(inferredType)) {
            needsRevisiting = true;
        }
        return inferredType;
    }

    private ExpressionType andSetIn(EvaluatorNode expression, ExpressionType type) {
        expression.unifyInferredTypeWith(type);
        return type;
    }
}
