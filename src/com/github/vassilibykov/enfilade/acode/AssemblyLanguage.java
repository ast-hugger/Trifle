// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.CallNode;
import com.github.vassilibykov.enfilade.core.ConstNode;
import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import com.github.vassilibykov.enfilade.core.VariableDefinition;
import com.github.vassilibykov.enfilade.core.VariableReferenceNode;
import com.github.vassilibykov.enfilade.expression.ExpressionLanguage;
import com.github.vassilibykov.enfilade.core.Primitive1Node;
import com.github.vassilibykov.enfilade.core.Primitive2Node;

public class AssemblyLanguage {

    public static Call call(CallNode callExpression) {
        return new Call(callExpression);
    }

    public static Drop drop() {
        return new Drop();
    }

    public static Goto jump(int address) {
        return new Goto(address);
    }

    public static Branch jump(EvaluatorNode test, int address) {
        return new Branch(test, address);
    }

    public static Load load(int value) {
        return new Load(new ConstNode(value));
    }

    public static Load load(String value) {
        return new Load(new ConstNode(value));
    }

    public static Load load(VariableDefinition var) {
        return new Load(new VariableReferenceNode(var));
    }

    public static Load load(Primitive1Node primitive) {
        return new Load(primitive);
    }

    public static Load load(Primitive2Node primitive) {
        return new Load(primitive);
    }

    public static Return ret() {
        return new Return();
    }

    public static Store store(VariableDefinition var) {
        return new Store(var);
    }

    private AssemblyLanguage() {}
}
