// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade;

import com.github.vassilibykov.enfilade.core.Environment;
import com.github.vassilibykov.enfilade.core.FunctionImplementation;
import com.github.vassilibykov.enfilade.core.FunctionTranslator;
import com.github.vassilibykov.enfilade.expression.Lambda;

import java.util.List;
import java.util.Optional;

public class Enfilade {
    public static void add(List<Lambda> functions) {
        functions.forEach(FunctionTranslator::translate);
    }

    public static Optional<FunctionImplementation> find(Lambda sourceFunction) {
        return Optional.ofNullable(Environment.INSTANCE.lookup(sourceFunction));
    }
}
