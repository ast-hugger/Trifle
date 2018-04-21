// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.builtins.BuiltinFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FreeFunctionCallDispatcher implements CallDispatcher {
    @NotNull private final FreeFunction target;

    public FreeFunctionCallDispatcher(@NotNull FreeFunction target) {
        this.target = target;
    }

    @Override
    public Invocable getInvocable(EvaluatorNode.Visitor<Object> visitor) {
        return target;
    }

    @Override
    public Optional<EvaluatorNode> evaluatorNode() {
        return Optional.empty();
    }

    @Override
    public JvmType generateCode(CallNode call, CodeGenerator generator) {
        if (target instanceof BuiltinFunction) {
            var name = ((BuiltinFunction) target).name();
            var callSiteType = generator.generateArgumentLoad(call);
            generator.writer().invokeDynamic(
                BuiltInFunctionCallInvokeDynamic.BOOTSTRAP,
                name,
                callSiteType);
            return JvmType.ofClass(callSiteType.returnType());
        } else if (target instanceof UserFunction) {
            var id = ((UserFunction) target).implementation().id(); // FIXME bogus
            var callSiteType = generator.generateArgumentLoad(call);
            generator.writer().invokeDynamic(
                UserFunctionCallInvokeDynamic.BOOTSTRAP,
                "freeCall",
                callSiteType,
                id);
            return JvmType.ofClass(callSiteType.returnType());
        } else {
            throw new AssertionError("unexpected dispatcher target: " + target);
        }
    }
}
