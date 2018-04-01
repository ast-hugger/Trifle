// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ACodeInterpreter implements ACodeInstruction.VoidVisitor {

    public static Object interpret(FunctionImplementation function) {
        Object[] frame = new Object[function.frameSize()];
        return on(function.acode, frame).interpret();
    }

    public static Object interpret(FunctionImplementation function, Object arg) {
        Object[] frame = new Object[function.frameSize()];
        frame[0] = arg;
        return on(function.acode, frame).interpret();
    }

    public static Object interpret(FunctionImplementation function, Object arg1, Object arg2) {
        Object[] frame = new Object[function.frameSize()];
        frame[0] = arg1;
        frame[1] = arg2;
        return on(function.acode, frame).interpret();
    }

    public static ACodeInterpreter on(ACodeInstruction[] code, Object[] frame) {
        return new ACodeInterpreter(code, frame, 0);
    }

    public static ACodeInterpreter on(ACodeInstruction[] code, Object[] frame, int initialPC) {
        return new ACodeInterpreter(code, frame, initialPC);
    }

    /**
     * Called by generated code to create an interpreter to recover from a specialization
     * failure (a square peg exception). The argument order is different from the usual
     * for this class because it allows us to generate better function epilogues.
     */
    @SuppressWarnings("unused") // called by generated code
    public static ACodeInterpreter forRecovery(int initialPC, Object[] frame, int functionId) {
        FunctionImplementation function = Objects.requireNonNull(FunctionRegistry.INSTANCE.lookup(functionId),
            "there is no function with ID " + functionId);
        ACodeInstruction[] code = Objects.requireNonNull(function.acode(),
            "function has no acode associated with it");
        return new ACodeInterpreter(code, frame, initialPC);
    }

    /*
        Instance
     */

    @NotNull private final ACodeInstruction[] code;
    @NotNull private final Object[] frame;
    private final Interpreter.Evaluator evaluator;
    private int pc;
    private Object register;

    ACodeInterpreter(@NotNull ACodeInstruction[] code, @NotNull Object[] frame, int initialPc) {
        this.code = code;
        this.frame = frame;
        this.pc = initialPc;
        this.evaluator = new Interpreter.Evaluator(frame);
    }

    public Object interpret() {
        while (pc >= 0) code[pc++].accept(this);
        return register;
    }

    @SuppressWarnings("unused") // called by generated code
    public Object interpret(Object registerValue) {
        register = registerValue;
        while (pc >= 0) code[pc++].accept(this);
        return register;
    }

    @Override
    public void visitBranch(ACodeInstruction.Branch anIf) {
        if ((Boolean) anIf.test.accept(evaluator)) {
            pc = anIf.address;
        }
    }

    @Override
    public void visitCall(ACodeInstruction.Call call) {
        register = call.callExpression.accept(evaluator);
    }

    @Override
    public void visitGoto(ACodeInstruction.Goto aGoto) {
        pc = aGoto.address;
    }

    @Override
    public void visitLoad(ACodeInstruction.Load load) {
        register = load.expression.accept(evaluator);
    }

    @Override
    public void visitReturn(ACodeInstruction.Return aReturn) {
        pc = -1;
    }

    @Override
    public void visitStore(ACodeInstruction.Store store) {
        frame[store.variable.index()] = register;
    }
}
