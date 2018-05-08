// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.lang.invoke.MethodType;
import java.util.NoSuchElementException;
import java.util.Optional;

public class DictionaryGetterDispatcher implements CallDispatcher {

    private final Dictionary dictionary;
    private final String key;

    public DictionaryGetterDispatcher(Dictionary dictionary, String key) {
        this.dictionary = dictionary;
        this.key = key;
    }

    @Override
    public Optional<EvaluatorNode> evaluatorNode() {
        return Optional.empty();
    }

    @Override
    public Object execute(CallNode call, EvaluatorNode.Visitor<Object> interpreter) {
        if (call.arity() != 0) {
            throw RuntimeError.message("invalid call expression"); // TODO should probably use a different exception
        }
        return dictionary.getEntry(key).orElseThrow(NoSuchElementException::new).value(); // TODO use a proper exception
    }

    @Override
    public Gist generateCode(CallNode call, CodeGenerator generator) {
        if (call.arity() != 0) {
            throw RuntimeError.message("invalid call expression"); // TODO should probably use a different exception
        }
        var returnType = call.specializedType();
        generator.writer().invokeDynamic(
            DictionaryAccessInvokeDynamic.BOOTSTRAP_GET,
            DictionaryAccessInvokeDynamic.getterName(key),
            MethodType.methodType(returnType.representativeClass()),
            dictionary.id());
        return returnType == JvmType.REFERENCE ? Gist.INFALLIBLE_REFERENCE : Gist.of(returnType, true);
    }
}
