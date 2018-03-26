// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Variable;
import org.jetbrains.annotations.TestOnly;

public class BackdoorsForTests {

    public static CallNode call(RuntimeFunction function, EvaluatorNode arg1, EvaluatorNode arg2) {
        return new CallNode.Call2(function, arg1, arg2);
    }

    public static VariableDefinition varDef(String name) {
        return new VariableDefinition(Variable.named(name));
    }

    @TestOnly
    public static void setVariableIndex(VariableDefinition var, int index) {
        var.index = index;
    }
}
