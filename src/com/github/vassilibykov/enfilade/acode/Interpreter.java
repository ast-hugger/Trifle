// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import org.jetbrains.annotations.NotNull;

public class Interpreter implements Instruction.VoidVisitor {

    @NotNull private Instruction[] code;
    @NotNull private Object[] frame;
    private final com.github.vassilibykov.enfilade.core.Interpreter.Evaluator evaluator;
    private int pc;
    private Object register;

    Interpreter(@NotNull Instruction[] code, @NotNull Object[] frame, int pc) {
        this.code = code;
        this.frame = frame;
        this.pc = pc;
        this.evaluator = new com.github.vassilibykov.enfilade.core.Interpreter.Evaluator(frame);
    }

    public Object interpret() {
        while (pc >= 0) {
            code[pc++].accept(this);
        }
        return register;
    }

    @Override
    public void visitBranch(Branch anIf) {
        if ((Boolean) anIf.test.accept(evaluator)) {
            pc = anIf.address;
        }
    }

    @Override
    public void visitCall(Call call) {
        register = call.callExpression.accept(evaluator);
    }

    @Override
    public void visitDrop(Drop drop) {
    }

    @Override
    public void visitGoto(Goto aGoto) {
        pc = aGoto.address;
    }

    @Override
    public void visitLoad(Load load) {
        register = load.expression.accept(evaluator);
    }

    @Override
    public void visitReturn(Return aReturn) {
        pc = -1;
    }

    @Override
    public void visitStore(Store store) {
        frame[store.variable.index()] = register;
    }
}
