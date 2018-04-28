// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.object;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.CallNode;
import com.github.vassilibykov.trifle.core.CodeGenerator;
import com.github.vassilibykov.trifle.core.EvaluatorNode;
import com.github.vassilibykov.trifle.core.Gist;
import com.github.vassilibykov.trifle.core.JvmType;
import com.github.vassilibykov.trifle.core.RuntimeError;

import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.stream.Collectors;

class MessageSendDispatcher implements CallDispatcher {

    private final String selector;

    MessageSendDispatcher(String selector) {
        this.selector = selector;
    }

    @Override
    public Optional<EvaluatorNode> evaluatorNode() {
        return Optional.empty();
    }

    @Override
    public Object execute(CallNode call, EvaluatorNode.Visitor<Object> interpreter) {
        if (call.arity() < 1) {
            throw RuntimeError.message("invalid message send expression; no receiver");
        }
        var args = call.arguments().map(each -> each.accept(interpreter)).toArray(Object[]::new);
        var receiver = args[0];
        if (!(receiver instanceof MessageReceiver)) {
            throw RuntimeError.message(receiver + " is not a message receiver");
        }
        var method = ((MessageReceiver) receiver).lookupSelector(selector);
        if (!method.isPresent()) {
            throw RuntimeError.message("message not understood: " + selector);
        }
        return method.get().invokeWithArguments(args);
    }

    @Override
    public Gist generateCode(CallNode call, CodeGenerator generator) {
        if (call.arity() < 1) {
            throw RuntimeError.message("invalid message send expression; no receiver");
        }
        var argTypes = call.arguments().map(each -> generator.generateCode(each).type()).collect(Collectors.toList());
        generator.writer().invokeDynamic(
            MessageSendInvokeDynamic.BOOTSTRAP,
            MessageSendInvokeDynamic.indyName(selector),
            MethodType.methodType(Object.class, argTypes.stream().map(JvmType::representativeClass).collect(Collectors.toList())));
        return Gist.INFALLIBLE_REFERENCE;
    }
}
