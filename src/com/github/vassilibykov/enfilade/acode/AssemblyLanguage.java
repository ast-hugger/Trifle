// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.AtomicExpression;
import com.github.vassilibykov.enfilade.core.CallExpression;
import com.github.vassilibykov.enfilade.core.ExpressionLanguage;
import com.github.vassilibykov.enfilade.core.Primitive1;
import com.github.vassilibykov.enfilade.core.Primitive2;
import com.github.vassilibykov.enfilade.core.Variable;

public class AssemblyLanguage {

    public static Call call(CallExpression callExpression) {
        return new Call(callExpression);
    }

    public static Drop drop() {
        return new Drop();
    }

    public static Goto jump(int address) {
        return new Goto(address);
    }

    public static Branch jump(AtomicExpression test, int address) {
        return new Branch(test, address);
    }

    public static Load load(int value) {
        return new Load(ExpressionLanguage.const_(value));
    }

    public static Load load(String value) {
        return new Load(ExpressionLanguage.const_(value));
    }

    public static Load load(Variable var) {
        return new Load(ExpressionLanguage.ref(var));
    }

    public static Load load(Primitive1 primitive) {
        return new Load(primitive);
    }

    public static Load load(Primitive2 primitive) {
        return new Load(primitive);
    }

    public static Return ret() {
        return new Return();
    }

    public static Store store(Variable var) {
        return new Store(var);
    }

    private AssemblyLanguage() {}
}
