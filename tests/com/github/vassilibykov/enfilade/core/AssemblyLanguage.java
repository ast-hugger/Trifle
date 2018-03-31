// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

public class AssemblyLanguage {

    public static ConstNode const_(Object value) {
        return new ConstNode(value);
    }

    public static ACodeInstruction.Call call(CallNode callExpression) {
        return new ACodeInstruction.Call(callExpression);
    }

    public static ACodeInstruction.Goto jump(int address) {
        return new ACodeInstruction.Goto(address);
    }

    public static ACodeInstruction.Branch jump(EvaluatorNode test, int address) {
        return new ACodeInstruction.Branch(test, address);
    }

    public static ACodeInstruction.Load load(int value) {
        return new ACodeInstruction.Load(new ConstNode(value));
    }

    public static ACodeInstruction.Load load(String value) {
        return new ACodeInstruction.Load(new ConstNode(value));
    }

    public static ACodeInstruction.Load load(VariableDefinition var) {
        return new ACodeInstruction.Load(new GetVariableNode(var));
    }

    public static ACodeInstruction.Load load(Closure closure) {
        return new ACodeInstruction.Load(ClosureNode.withNoCopiedValues(closure.implementation));
    }

    public static GetVariableNode ref(VariableDefinition var) {
        return new GetVariableNode(var);
    }

    public static ACodeInstruction.Load load(Primitive1Node primitive) {
        return new ACodeInstruction.Load(primitive);
    }

    public static ACodeInstruction.Load load(Primitive2Node primitive) {
        return new ACodeInstruction.Load(primitive);
    }

    public static ACodeInstruction.Return ret() {
        return new ACodeInstruction.Return();
    }

    public static ACodeInstruction.Store store(VariableDefinition var) {
        return new ACodeInstruction.Store(var);
    }

    private AssemblyLanguage() {}
}