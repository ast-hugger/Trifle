// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.lang.invoke.MethodType;

public interface CodeGenerator extends EvaluatorNode.Visitor<Gist> {
    GhostWriter writer();
    Gist generateCode(EvaluatorNode node);
    MethodType generateArgumentLoad(CallNode callNode);
}
