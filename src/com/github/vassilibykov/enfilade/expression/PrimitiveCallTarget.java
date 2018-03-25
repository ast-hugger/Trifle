// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import com.github.vassilibykov.enfilade.core.EvaluatorNode;

import java.util.List;

public interface PrimitiveCallTarget {
    EvaluatorNode translate(List<EvaluatorNode> args);
}
