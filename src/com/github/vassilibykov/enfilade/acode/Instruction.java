// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

public abstract class Instruction {

    public interface VoidVisitor {
        void visitCall(Call call);
        void visitDrop(Drop drop);
        void visitGoto(Goto aGoto);
        void visitIf(If anIf);
        void visitLoad(Load load);
        void visitReturn(Return aReturn);
        void visitStore(Store store);
    }

    public abstract void accept(VoidVisitor visitor);
}
