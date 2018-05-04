// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

import com.github.vassilibykov.trifle.core.CallDispatcher;
import com.github.vassilibykov.trifle.core.Dictionary;
import com.github.vassilibykov.trifle.core.DictionaryGetterDispatcher;
import com.github.vassilibykov.trifle.core.EvaluatorNode;

public class DictionaryGetter implements Callable {

    public static DictionaryGetter create(Dictionary dictionary, String key) {
        return new DictionaryGetter(dictionary, key);
    }

    private final Dictionary dictionary;
    private final String key;

    private DictionaryGetter(Dictionary dictionary, String key) {
        this.dictionary = dictionary;
        this.key = key;
    }

    public Dictionary dictionary() {
        return dictionary;
    }

    public String key() {
        return key;
    }

    @Override
    public CallDispatcher createDispatcher(Visitor<EvaluatorNode> translator) {
        return new DictionaryGetterDispatcher(dictionary, key);
    }
}
