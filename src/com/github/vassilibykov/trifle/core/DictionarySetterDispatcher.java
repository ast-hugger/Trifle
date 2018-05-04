// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.lang.invoke.MethodType;
import java.util.Optional;

public class DictionarySetterDispatcher implements CallDispatcher {

    private final Dictionary dictionary;
    private final String key;

    public DictionarySetterDispatcher(Dictionary dictionary, String key) {
        this.dictionary = dictionary;
        this.key = key;
    }

    @Override
    public Optional<EvaluatorNode> evaluatorNode() {
        return Optional.empty();
    }

    @Override
    public Object execute(CallNode call, EvaluatorNode.Visitor<Object> interpreter) {
        if (call.arity() != 1) {
            throw RuntimeError.message("invalid call expression"); // TODO should probably use a different exception
        }
        var value = call.argument(0).accept(interpreter);
        dictionary.getEntry(key).setValue(value);
        return value;
    }

    @Override
    public Gist generateCode(CallNode call, CodeGenerator generator) {
        if (call.arity() != 1) {
            throw RuntimeError.message("invalid call expression"); // TODO should probably use a different exception
        }
        var gist = generator.generateCode(call.argument(0)); // argument is primitive; can't fail
        generator.writer().pop(); // leave the value on the stack as the result
        generator.writer().invokeDynamic(
            DictionaryAccessInvokeDynamic.BOOTSTRAP_SET,
            DictionaryAccessInvokeDynamic.setterName(key),
            MethodType.methodType(void.class, gist.type().representativeClass()),
            dictionary.id());
        return gist;
    }
}
