// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.stream.Stream;

/**
 * Analyzes a function body after it has been evaluated a number of times by the
 * {@link ProfilingInterpreter}. Populates {@link CompilerAnnotation}s attached
 * to expressions with types observed by the interpreter.
 *
 * <p>Each compiler annotation informs the compiler about the values the
 * associated expression can take. For simplicity below we refer to them as
 * "types", however we really mean a broader {@link JvmType}: whether the
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
class ExpressionTypeObserver implements EvaluatorNode.Visitor<ExpressionType> {
    private static final ExpressionType UNKNOWN = ExpressionType.unknown();

    static void analyze(RuntimeFunction function) {
        ExpressionTypeObserver observer = new ExpressionTypeObserver(function);
        Stream.of(function.arguments()).forEach(
            each -> each.compilerAnnotation.setObservedType(each.profile.observedType()));
        function.body().accept(observer);
    }

    /*
        Instance
     */

    private final EvaluatorNode functionBody;

    private ExpressionTypeObserver(RuntimeFunction function) {
        this.functionBody = function.body();
    }

    @Override
    public ExpressionType visitCall0(CallNode.Call0 call) {
        if (call.profile.hasProfileData()) {
            return setKnownType(call, call.profile.valueCategory());
        } else {
            return UNKNOWN;
        }
    }

    @Override
    public ExpressionType visitCall1(CallNode.Call1 call) {
        call.arg().accept(this);
        if (call.profile.hasProfileData()) {
            return setKnownType(call, call.profile.valueCategory());
        } else {
            return UNKNOWN;
        }
    }

    @Override
    public ExpressionType visitCall2(CallNode.Call2 call) {
        call.arg1().accept(this);
        call.arg2().accept(this);
        if (call.profile.hasProfileData()) {
            return setKnownType(call, call.profile.valueCategory());
        } else {
            return UNKNOWN;
        }
    }

    /**
     * While we know with certainty what type a const <em>could</em> be observed
     * to produce, we cannot claim that it was <em>observed</em> to produce it.
     * We could only do so if we tracked whether a constant has in fact been
     * evaluated.
     */
    @Override
    public ExpressionType visitConst(ConstNode aConst) {
        return aConst.hasBeenEvaluated()
            ? setKnownType(aConst, JvmType.ofObject(aConst.value()))
            : UNKNOWN;
    }

    @Override
    public ExpressionType visitIf(IfNode anIf) {
        anIf.condition().accept(this);
        ExpressionType trueType = anIf.trueBranch().accept(this);
        ExpressionType falseType = anIf.falseBranch().accept(this);
        ExpressionType unified = trueType.opportunisticUnion(falseType);
        anIf.unifyObservedTypeWith(unified);
        return unified;
    }

    /**
     * See the note in {@link #visitVarSet(SetVariableNode)}. We descend into the init
     * expression because it must be processed, but are not concerned with the
     * type reported by the descent. That type is already reflected in the
     * empirical variable profile.
     */
    @Override
    public ExpressionType visitLet(LetNode let) {
        let.initializer().accept(this);
        VariableDefinition var = let.variable();
        var.compilerAnnotation.unifyObservedTypeWith(var.profile.observedType());
        ExpressionType bodyType = let.body().accept(this);
        let.unifyObservedTypeWith(bodyType);
        return bodyType;
    }

    /**
     * Same as for {@link #visitConst(ConstNode)}, we know the type but we can't
     * claim we've observed the primitive produce it.
     */
    @Override
    public ExpressionType visitPrimitive1(Primitive1Node primitive) {
        primitive.argument().accept(this);
        return primitive.hasBeenEvaluated()
            ? setKnownType(primitive, primitive.valueCategory())
            : UNKNOWN;
    }

    @Override
    public ExpressionType visitPrimitive2(Primitive2Node primitive) {
        primitive.argument1().accept(this);
        primitive.argument2().accept(this);
        return primitive.hasBeenEvaluated()
            ? setKnownType(primitive, primitive.valueCategory())
            : UNKNOWN;
    }

    @Override
    public ExpressionType visitBlock(BlockNode block) {
        ExpressionType type = ExpressionType.known(JvmType.REFERENCE);
        for (EvaluatorNode each : block.expressions()) {
            type = each.accept(this);
        }
        block.unifyObservedTypeWith(type);
        return type;
    }

    /**
     * The observed type of the return expression is folded into the function
     * body type, while the return itself has the void type.
     */
    @Override
    public ExpressionType visitRet(ReturnNode ret) {
        ExpressionType valueType = ret.value().accept(this);
        functionBody.unifyObservedTypeWith(valueType);
        return setKnownType(ret, JvmType.VOID);
    }

    @Override
    public ExpressionType visitVarRef(VariableReferenceNode varRef) {
        ExpressionType observed;
        if (varRef.hasBeenEvaluated()) {
            observed = varRef.variable.compilerAnnotation.observedType();
        } else {
            observed = ExpressionType.unknown();
        }
        varRef.unifyObservedTypeWith(observed);
        return observed;
    }

    /**
     * The observed value of the new value expression does not need to be
     * iteratively unified with the current observed type of the variable the
     * way the inferencer does it in {@link
     * ExpressionTypeInferencer#visitVarSet(SetVariableNode)}. The observed type of
     * a variable by definition already includes everything the value
     * expression has been known to produce.
     */
    @Override
    public ExpressionType visitVarSet(SetVariableNode set) {
        ExpressionType valueType = set.value().accept(this);
        set.unifyObservedTypeWith(valueType);
        return valueType;
    }

    private ExpressionType setKnownType(EvaluatorNode expression, JvmType type) {
        ExpressionType expressionType = ExpressionType.known(type);
        expression.unifyObservedTypeWith(expressionType);
        return expressionType;
    }
}
