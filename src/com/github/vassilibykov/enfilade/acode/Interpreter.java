// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.acode;

import com.github.vassilibykov.enfilade.core.RuntimeFunction;
import com.github.vassilibykov.enfilade.core.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Interpreter implements Instruction.VoidVisitor {

    public static Interpreter on(Instruction[] code) {
        return new Interpreter(code, new Object[0], 0);
    }

    public static Interpreter on(Instruction[] code, Object[] frame) {
        return new Interpreter(code, frame, 0);
    }

    public static Interpreter on(Instruction[] code, Object[] frame, int initialPC) {
        return new Interpreter(code, frame, initialPC);
    }

    /**
     * Called by generated code to create an interpreter to recover from a
     * specialization failure (a square peg exception). The order of the
     * arguments is different from the customary for this class because it
     * allows us to generate better function epilogues.
     */
    @SuppressWarnings("unused") // called by generated code
    public static Interpreter forRecovery(int initialPC, Object[] frame, int functionId) {
        RuntimeFunction function = Objects.requireNonNull(Environment.INSTANCE.lookup(functionId),
            "there is no function with ID " + functionId);
        Instruction[] code = Objects.requireNonNull(function.acode(),
            "function has no acode associated with it");
        return new Interpreter(code, frame, initialPC);
    }

    /*
        Instance
     */

    @NotNull private Instruction[] code;
    @NotNull private Object[] frame;
    private final com.github.vassilibykov.enfilade.core.Interpreter.Evaluator evaluator;
    private int pc;
    private Object register;

    Interpreter(@NotNull Instruction[] code, @NotNull Object[] frame, int initialPc) {
        this.code = code;
        this.frame = frame;
        this.pc = initialPc;
        this.evaluator = new com.github.vassilibykov.enfilade.core.Interpreter.Evaluator(frame);
    }

    public Object interpret() {
        while (pc >= 0) {
            code[pc++].accept(this);
        }
        return register;
    }

    public Object interpret(Object registerValue) {
        register = registerValue;
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
        frame[store.variable.genericIndex()] = register;
    }
}
