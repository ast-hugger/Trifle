// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import com.github.vassilibykov.trifle.builtin.BuiltinFunction;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class FreeFunctionCallDispatcher implements CallDispatcher {
    @NotNull private final FreeFunction target;

    public FreeFunctionCallDispatcher(@NotNull FreeFunction target) {
        this.target = target;
    }

    @Override
    public Object execute(CallNode call, EvaluatorNode.Visitor<Object> interpreter) {
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

            @Override
            public Object ifTernary(EvaluatorNode arg1, EvaluatorNode arg2, EvaluatorNode arg3) {
                return target.invoke(arg1.accept(interpreter), arg2.accept(interpreter), arg3.accept(interpreter));
            }

            @Override
            public Object ifQuaternary(EvaluatorNode arg1, EvaluatorNode arg2, EvaluatorNode arg3, EvaluatorNode arg4) {
                return target.invoke(
                    arg1.accept(interpreter),
                    arg2.accept(interpreter),
                    arg3.accept(interpreter),
                    arg4.accept(interpreter));
            }

            @Override
            public Object ifMultifarious(EvaluatorNode[] args) {
                var arguments = Stream.of(args).map(each -> each.accept(interpreter)).toArray();
                return target.invokeWithArguments(arguments);
            }
        });
    }

    @Override
    public Gist generateCode(CallNode call, CodeGenerator generator) {
        if (target instanceof BuiltinFunction) {
            var name = ((BuiltinFunction) target).name();
            var callSiteType = generator.generateArgumentLoad(call);
            generator.writer().invokeDynamic(
                BuiltInFunctionCallInvokeDynamic.BOOTSTRAP,
                name,
                callSiteType);
            var returnType = JvmType.ofClass(callSiteType.returnType());
            return Gist.of(returnType, returnType != JvmType.REFERENCE);
        } else if (target instanceof UserFunction) {
            var userFunction = (UserFunction) this.target;
            var id = userFunction.implementation().id();
            var callSiteType = generator.generateArgumentLoad(call);
            generator.writer().invokeDynamic(
                UserFunctionCallInvokeDynamic.BOOTSTRAP,
                userFunction.name(),
                callSiteType,
                id);
            var returnType = JvmType.ofClass(callSiteType.returnType());
            return Gist.of(returnType, returnType != JvmType.REFERENCE);
        } else {
            throw new AssertionError("unexpected dispatcher target: " + target);
        }
    }

    @Override
    public String toString() {
        return target.name();
    }
}
