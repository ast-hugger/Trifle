// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.stream.Stream;

/**
 * Analyzes a function body after it has been evaluated a number of times by the
 * {@link ProfilingInterpreter}. Creates {@link CompilerAnnotation} instances
 * and stores them in expression nodes for later use by the code generator.
 *
 * <p>Each compiler annotation informs the compiler about the values the
 * associated expression can take. For simplicity below we refer to them as
 * "types", however we really mean a broader {@link TypeCategory}: whether the
 * value is a reference or one of the primitive types, and which one. How the
 * information is obtained depends on the specific node.
 *
 * <p>An expression type may be called deterministic if the expression can never
 * produce a value not of that type. Thus a REFERENCE type is always
 * deterministic, while a primitive type can be deterministic only if it
 * can be statically inferred from the expression.
 *
 * <p>The type of a constant or a primitive can always be inferred statically
 * and is thus always deterministic.
 *
 * <p>The type of variable is in the general case based on profile information
 * and is not deterministic (unless, as mentioned above, the profiled type is
 * REFERENCE). That is always true for a variable used as a function argument. A
 * let-bound variable may, in fact, have a deterministic statically known type
 * if the value expression is itself of a deterministic type. If profile
 * information is missing (which can only happen to let-bound variables on code
 * branches never hit during profiling), the variable is assigned the REFERENCE
 * type.
 *
 * <p>The type of a call expression is always based on profile information and is
 * never deterministic. The profile information may be missing if the call is on
 * a conditional branch that has never been taken while profiling. If that is
 * the case, the call is assigned the REFERENCE type.
 *
 * <p>The type of an {@code if} expression is the union of types of its two
 * branches. It is deterministic if the type of both branches is deterministic.
 *
 * <p>The type of a {@code let} expression is the type of its body.
 *
 * <p>The type of a {@code prog} expression is the type of its last
 * subexpression, or a deterministic REFERENCE type if the expression is empty,
 * to account for the {@code null} value it evaluates to in that case.
 *
 * <p>The type of a {@code setVar} expression is the type of its newValue
 * subexpression. The subexpression is required to be atomic, but because
 * atomic expressions include variable references, the type may still be
 * non-deterministic.
 *
 * <p>The type of a {@code ret} expression is the type of its value. It is
 * special in that values the type describes are passed not to the lexically
 * apparent continuation of the ret expression, but rather to the continuation
 * of the enclosing function. Thus, the profile of a function return value
 * recorded by the function actually reflects the values produced by the
 * function body and any {@code ret} expressions it contains.
 *
 * <p>Formally, the return type of a function is a union of the type of its
 * body and all {@code ret} expressions it contains. That type is
 * deterministic if the all the involved types are deterministic.
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
        // FIXME
//        expression.setCompilerAnnotation(new CompilerAnnotation(category));
        return category;
    }
}
