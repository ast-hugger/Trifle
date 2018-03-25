// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

public interface Visitor<T> {
    T visitBlock(Block block);
    T visitCall(Call call);
    T visitConst(Const aConst);
    T visitIf(If anIf);
    T visitLet(Let let);
    T visitPrimitive();
    T visitReturn(Return aReturn);
    T visitSetVariable(SetVariable setVariable);
    T visitVariable(Variable variable);
}
