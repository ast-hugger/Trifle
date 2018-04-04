// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

public interface Visitor<T> {
    T visitBlock(Block block);
    T visitCall(Call call);
    T visitConst(Const aConst);
    T visitIf(If anIf);
    T visitLambda(Lambda lambda);
    T visitLet(Let let);
    T visitLetrec(Letrec letrec);
    T visitPrimitiveCall(PrimitiveCall primitiveCall);
    T visitReturn(Return aReturn);
    T visitSetVariable(SetVariable setVariable);
    T visitTopLevelBinding(TopLevel.Binding binding);
    T visitVariable(Variable variable);
}
