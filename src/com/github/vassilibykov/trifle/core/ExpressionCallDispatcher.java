// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ExpressionCallDispatcher implements CallDispatcher {
    @NotNull private final EvaluatorNode expression;

    public ExpressionCallDispatcher(@NotNull EvaluatorNode expression) {
        this.expression = expression;
    }

    @Override
    public Optional<EvaluatorNode> asEvaluatorNode() {
        return Optional.of(expression);
    }

    @Override
    public Object execute(CallNode call, EvaluatorNode.Visitor<Object> interpreter) {
        Invocable target;
        try {
            target = (Invocable) expression.accept(interpreter);
        } catch (ClassCastException e) {
            throw RuntimeError.message("closure expected");
        }
        return call.match(new CallNode.ArityMatcher<>() {
            @Override
            public Object ifNullary() {
                return target.invoke();
            }

            @Override
            public Object ifUnary(EvaluatorNode arg) {
                return target.invoke(arg.accept(interpreter));
            }

            @Override
            public Object ifBinary(EvaluatorNode arg1, EvaluatorNode arg2) {
                return target.invoke(arg1.accept(interpreter), arg2.accept(interpreter));
            }
        });
    }

    @Override
    public Gist generateCode(CallNode call, CodeGenerator generator) {
        var functionType = generator.generateCode(expression);
        generator.writer().ensureValue(functionType.type(), JvmType.REFERENCE);
        var callSiteType = generator.generateArgumentLoad(call);
        callSiteType = callSiteType.insertParameterTypes(0, Object.class); // the leading closure
        generator.writer().invokeDynamic(ExpressionCallInvokeDynamic.BOOTSTRAP, "call", callSiteType);
        var returnType = JvmType.ofClass(callSiteType.returnType());
        return Gist.of(returnType, returnType != JvmType.REFERENCE);
    }
}
