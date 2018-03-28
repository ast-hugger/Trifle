// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/*
 * <p>A-code is a representation of A-normal forms in a shape resembling a
 * sequence of instructions. It can be executed by A-machine which consists
 * of a vector of local variable values, a current instruction pointer,
 * and a single register (or if you will, a stack of maximum depth 1).
 *
 * <p>There are only seven instructions. Instruction arguments, when present,
 * are actual objects of the specified types.
 *
 * <ul>
 *     <li>load EvaluatorNode</li>
 *     <li>call CallNode</li>
 *     <li>store VariableDefinition</li>
 *     <li>drop</li>
 *     <li>branch EvaluatorNode int</li>
 *     <li>goto int</li>
 *     <li>return</li>
 * </ul>
 */
public abstract class ACodeInstruction {

    public interface VoidVisitor {
        void visitBranch(Branch branch);
        void visitCall(Call call);
        void visitDrop(Drop drop);
        void visitGoto(Goto aGoto);
        void visitLoad(Load load);
        void visitReturn(Return aReturn);
        void visitStore(Store store);
    }

    public abstract void accept(VoidVisitor visitor);

    /**
     * Set the instruction pointer to the specified address if the test evaluates to
     * true.
     */
    public static class Branch extends ACodeInstruction {
        /*internal*/ final EvaluatorNode test;
        /*internal*/ int address;

        Branch(EvaluatorNode test, int address) {
            this.test = test;
            this.address = address;
        }

        @Override
        public void accept(VoidVisitor visitor) {
            visitor.visitBranch(this);
        }

        @Override
        public String toString() {
            return "BRANCH " + test + " " + address;
        }
    }

    /**
     * Perform the call and set the register to contain the result.
     */
    public static class Call extends ACodeInstruction {
        @NotNull /*internal*/ final CallNode callExpression;

        Call(@NotNull CallNode callExpression) {
            this.callExpression = callExpression;
        }

        @Override
        public void accept(VoidVisitor visitor) {
            visitor.visitCall(this);
        }

        @Override
        public String toString() {
            return "CALL #" + callExpression.function();
        }
    }

    /**
     * Discard the current value of the register. If the value register is
     * implemented as a register and not a 1-value stack, this is essentially a
     * no-op.
     */
    public static class Drop extends ACodeInstruction {
        Drop() {
        }

        @Override
        public void accept(VoidVisitor visitor) {
            visitor.visitDrop(this);
        }

        @Override
        public String toString() {
            return "DROP";
        }
    }

    /**
     * Unconditionally set the instruction pointer to the specified address.
     */
    public static class Goto extends ACodeInstruction {
        /*internal*/ int address;

        Goto(int address) {
            this.address = address;
        }

        @Override
        public void accept(VoidVisitor visitor) {
            visitor.visitGoto(this);
        }

        @Override
        public String toString() {
            return "GOTO " + address;
        }
    }

    /**
     * Evaluate the atomic expression and set the value register to contain the result.
     */
    public static class Load extends ACodeInstruction {
        @NotNull /*internal*/ final EvaluatorNode expression;

        Load(@NotNull EvaluatorNode expression) {
            this.expression = expression;
        }

        @Override
        public void accept(VoidVisitor visitor) {
            visitor.visitLoad(this);
        }

        @Override
        public String toString() {
            return "LOAD " + expression;
        }
    }

    /**
     * Return the value of the register as the result of this invocation.
     */
    public static class Return extends ACodeInstruction {
        Return() {
        }

        @Override
        public void accept(VoidVisitor visitor) {
            visitor.visitReturn(this);
        }

        @Override
        public String toString() {
            return "RETURN";
        }
    }

    /**
     * Store the value of the register in the specified local variable.
     */
    public static class Store extends ACodeInstruction {
        @NotNull /*internal*/ final VariableDefinition variable;

        Store(@NotNull VariableDefinition variable) {
            this.variable = variable;
        }

        @Override
        public void accept(VoidVisitor visitor) {
            visitor.visitStore(this);
        }

        @Override
        public String toString() {
            return "STORE " + variable;
        }
    }
}
