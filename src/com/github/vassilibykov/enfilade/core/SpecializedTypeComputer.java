// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;

/**
 * Processes profile data after a function has been evaluated a number of times
 * by the {@link ProfilingInterpreter}. Populates evaluator nodes with {@link
 * ExpressionType}s representing the types of values they were observed to
 * produce.
 */
class SpecializedTypeComputer implements EvaluatorNode.Visitor<JvmType> {

    /**
     * Set {@code specializedType} fields of variable and evaluator nodes in a
     * top-level function and all of its closure implementation functions.
     *
     * @param useGenericSignature If true, force all function parameters and
     *        results specialization types to be {@code Object} no matter their
     *        observed values. Otherwise, set their specializations according
     *        to their observed values.
     * @param topLevelFunction The top function of a closure tree to process.
     */
    static void process(boolean useGenericSignature, FunctionImplementation topLevelFunction) {
        if (!topLevelFunction.isTopLevel()) throw new IllegalArgumentException("not a top-level function");
        processFunction(useGenericSignature, topLevelFunction);
        topLevelFunction.closureImplementations().forEach(each -> processFunction(useGenericSignature, each));
    }

    private static void processFunction(boolean isForGenericSignature, FunctionImplementation function) {
        var computer = new SpecializedTypeComputer(isForGenericSignature, function);
        computer.process();
    }

    /*
        Instance
     */

    private final boolean useGenericSignature;
    private final FunctionImplementation function;

    private SpecializedTypeComputer(boolean useGenericSignature, FunctionImplementation function) {
        this.useGenericSignature = useGenericSignature;
        this.function = function;
    }

    private void process() {
        for (var eachParam : function.allParameters()) {
            eachParam.setSpecializedType(effectiveTypeInSignature(eachParam.profile().observedType()));
        }
        function.body().accept(this);
        function.setSpecializedReturnType(effectiveTypeInSignature(function.profile.resultProfile().observedType()));
    }
    
    private JvmType effectiveTypeInSignature(ExpressionType type) {
        return useGenericSignature ? REFERENCE : type.jvmType().orElse(REFERENCE);
    }

    @Override
    public JvmType visitCall0(CallNode.Call0 call) {
        call.dispatcher().evaluatorNode().ifPresent(it -> it.accept(this));
        if (call.profile.hasProfileData()) {
            return setSpecializedType(call, call.profile.jvmType());
        } else {
            return setSpecializedType(call, REFERENCE);
        }
    }

    @Override
    public JvmType visitCall1(CallNode.Call1 call) {
        call.dispatcher().evaluatorNode().ifPresent(it -> it.accept(this));
        call.arg().accept(this);
        if (call.profile.hasProfileData()) {
            return setSpecializedType(call, call.profile.jvmType());
        } else {
            return setSpecializedType(call, REFERENCE);
        }
    }

    @Override
    public JvmType visitCall2(CallNode.Call2 call) {
        call.dispatcher().evaluatorNode().ifPresent(it -> it.accept(this));
        call.arg1().accept(this);
        call.arg2().accept(this);
        if (call.profile.hasProfileData()) {
            return setSpecializedType(call, call.profile.jvmType());
        } else {
            return setSpecializedType(call, REFERENCE);
        }
    }

    /**
     * What's set as observed type for a closure has not strictly speaking been
     * observed. We don't profile the evaluation of closure nodes because their
     * type is statically known and can never be anything else. So, a closure node
     * might never have been evaluated but will still have a known "observed" type.
     */
    @Override
    public JvmType visitClosure(ClosureNode closure) {
        return setSpecializedType(closure, REFERENCE);
    }

    /**
     * What has been said about the "observed" type of closures also applies
     * to constants.
     *
     * @see #visitClosure(ClosureNode)
     */
    @Override
    public JvmType visitConstant(ConstantNode aConst) {
        return setSpecializedType(aConst, aConst.inferredType().jvmType().orElse(REFERENCE));
    }

    /**
     * The observed type of an {@code if} is in theory a union of the observed
     * types of its branches. It's an opportunistic (lower bound with respect to
     * REFERENCE) union so that if one of the types is unknown, the union is the
     * other type.
     *
     * <p>In practice, because we don't track evaluation of expressions whose type
     * is statically known, a branch may claim to have a certain observed type when
     * in practice it never has been evaluated. We correct that by tracking whether
     * a branch has been evaluated. Without this precaution, a branch of a known type
     * which is never evaluated could prevent an opportunity to specialize to the
     * other branch type.
     */
    @Override
    public JvmType visitIf(IfNode anIf) {
        anIf.condition().accept(this);
        var trueType = anIf.trueBranch().accept(this);
        var falseType = anIf.falseBranch().accept(this);
        var effectiveTrueType = anIf.trueBranchCount.get() > 0
            ? ExpressionType.known(trueType)
            : ExpressionType.unknown();
        var effectiveFalseType = anIf.falseBranchCount.get() > 0
            ? ExpressionType.known(falseType)
            : ExpressionType.unknown();
        var unified = effectiveTrueType.opportunisticUnion(effectiveFalseType);
        return setSpecializedType(anIf, unified.jvmType().orElse(REFERENCE));
    }

    /**
     * See the note in {@link #visitSetVar(SetVariableNode)}. We descend into the init
     * expression because it must be processed, but are not concerned with the
     * type reported by the descent. That type is already reflected in the
     * empirical variable profile.
     */
    @Override
    public JvmType visitLet(LetNode let) {
        var initType = let.initializer().accept(this);
        var var = let.variable();
        var observedType = var.profile.observedType();
        JvmType type = observedType.isUnknown()
            ? initType
            : observedType.jvmType().orElse(REFERENCE);
        var.setSpecializedType(type);
        var bodyType = let.body().accept(this);
        return setSpecializedType(let, bodyType);
    }

    /**
     * Same as for {@link #visitConstant(ConstantNode)}, we know the type but we can't
     * claim we've observed the primitive produce it.
     */
    @Override
    public JvmType visitPrimitive1(Primitive1Node primitive) {
        var argType = ExpressionType.known(primitive.argument().accept(this));
        var resultType = primitive.implementation().inferredType(argType).jvmType().orElse(REFERENCE);
        return setSpecializedType(primitive, resultType);
    }

    @Override
    public JvmType visitPrimitive2(Primitive2Node primitive) {
        var arg1Type = ExpressionType.known(primitive.argument1().accept(this));
        var arg2Type = ExpressionType.known(primitive.argument2().accept(this));
        var resultType = primitive.implementation().inferredType(arg1Type, arg2Type).jvmType().orElse(REFERENCE);
        return setSpecializedType(primitive, resultType);
    }

    @Override
    public JvmType visitBlock(BlockNode block) {
        var type = REFERENCE; // tentative, for an empty block
        for (EvaluatorNode each : block.expressions()) type = each.accept(this);
        return setSpecializedType(block, type);
    }

    /**
     * The type of the return expression, if it actually was evaluated, is
     * already reflected in the observed type of function body.
     */
    @Override
    public JvmType visitReturn(ReturnNode ret) {
        ret.value().accept(this);
        return setSpecializedType(ret, JvmType.VOID);
    }

    @Override
    public JvmType visitGetVar(GetVariableNode varRef) {
        var observed = varRef.variable().specializedType();
        return setSpecializedType(varRef, observed);
    }

    @Override
    public JvmType visitSetVar(SetVariableNode set) {
        var valueType = set.value().accept(this);
        return setSpecializedType(set, valueType);
    }

    @Override
    public JvmType visitFreeFunctionReference(FreeFunctionReferenceNode constFunction) {
        return setSpecializedType(constFunction, REFERENCE);
    }

    private JvmType setSpecializedType(EvaluatorNode expression, JvmType type) {
        expression.setSpecializedType(type);
        return type;
    }
}
