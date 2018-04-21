// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ExpressionCallDispatcher implements CallDispatcher {
    @NotNull private final EvaluatorNode expression;

    public ExpressionCallDispatcher(@NotNull EvaluatorNode expression) {
        this.expression = expression;
    }

    @Override
    public Optional<EvaluatorNode> evaluatorNode() {
        return Optional.of(expression);
    }

    @Override
    public Invocable getInvocable(EvaluatorNode.Visitor<Object> visitor) {
        try {
            return (Invocable) expression.accept(visitor);
        } catch (ClassCastException e) {
            throw RuntimeError.message("closure expected");
        }
    }

    @Override
    public JvmType generateCode(CallNode call, CodeGenerator generator) {
        var functionType = generator.generateCode(expression);
        generator.writer().ensureValue(functionType, JvmType.REFERENCE);
        var callSiteType = generator.generateArgumentLoad(call);
        callSiteType = callSiteType.insertParameterTypes(0, Object.class); // the leading closure
        generator.writer().invokeDynamic(ExpressionCallInvokeDynamic.BOOTSTRAP, "call", callSiteType);
        return JvmType.ofClass(callSiteType.returnType());
    }
}
