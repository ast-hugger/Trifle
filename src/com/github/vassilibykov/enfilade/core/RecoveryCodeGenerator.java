// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;

/**
 * A code generator producing recovery code of a function implementation. The
 * code follows the "normal" code generated by {@link MethodCodeGenerator} in
 * the same JVM method. An SPE handler jumps into the recovery code after
 * unwrapping the SPE to have its value on the stack and "unspecializing" any
 * live locals (replacing primitive values with wrapper objects).
 *
 * <p>The simple way to generate recovery code would be with a regular {@link
 * EvaluatorNode.Visitor}, generating an entire second copy of the function with
 * all operations being generic. However, because recovery code is only
 * accessible via jumps from SPE handlers, large portions of it end up being
 * dead code and are erased by ASM, replaced with unsightly swathes of {@code
 * nop}s. To generate a more eye-pleasing and compact result, we follow a
 * somewhat more involved approach.
 *
 * <p>We convert the original tree of {@link EvaluatorNode}s of the function
 * into an equivalent form called here A-code. A-code is a hybrid representation
 * of the original A-normal forms, such that atomic expressions retain their
 * tree form, while complex expressions are translated into a sequence of
 * instructions. In this representation, we can easily analyze control flow and
 * eliminate dead code. The actual bytecode of the recovery code is generated
 * from this representation.
 *
 * <p>Execution of A-code can be formalized as a machine consisting of a
 * sequence of instructions, the current instruction pointer, and a single
 * register containing either an Object (including null) or being empty (which
 * is different from containing null). Alternatively, the register could be
 * regarded as a stack of maximum depth 1. The instructions are:
 *
 * <p><b>load EvaluatorNode</b> - The node may be an atomic expression or a call.
 * Evaluates the expression and stores the result in the register.
 *
 * <p><b>store VariableDefinition</b> - Stores the value in the register into
 * the variable. The register must not be empty, and becomes empty after
 * executing the instruction.
 *
 * <p><b>copy VariableDefinition</b> - Same as <b>store</b>, but the register
 * is not emptied after executing the instruction.
 *
 * <p><b>branch EvaluatorNode int</b> - The node must be an atomic expression.
 * Evaluates the expression, which must produce a Boolean. If the result is
 * true, the instruction pointer is set to the {@code int} operand. In other
 * words, this is a conditional branch on true to an absolute address.
 * The register is not affected by this instruction.
 *
 * <p><b>goto int</b> - Unconditionally set the instruction pointer to the value
 * of the operand. The register is not affected.
 *
 * <p><b>return</b> - Return the value in the register (which must not be
 * empty) as the result of the function.
 *
 * <p>The essence of this transformation is to make explicit the saving of
 * values the computation of which may have failed in the "normal" code ({@code
 * store}, {@code copy}, and {@code return} instructions). These save points
 * are targets of control transfers from normal code. This representation
 * also linearizes the flow of control in non-atomic code, thus allowing to
 * detect and eliminate dead code.
 *
 * <p>The intermediate representation is lazily computed and cached by
 * {@link FunctionImplementation#recoveryCode()}, because it is the same for
 * both generic and specialized forms of a function, and computing it
 * involves some work.
 */
class RecoveryCodeGenerator {

    static abstract class Instruction {
        private boolean isLive;
        @Nullable Label incomingJumpLabel;
        abstract void accept(RecoveryCodeGenerator visitor);
    }

    private static abstract class JumpInstruction extends Instruction {
        int address;
        Instruction target;

        private JumpInstruction(int address) {
            this.address = address;
        }
    }

    /**
     * Set the instruction pointer to the specified address if the test evaluates to
     * true.
     */
    private static class Branch extends JumpInstruction {
        final EvaluatorNode test;

        Branch(EvaluatorNode test, int address) {
            super(address);
            this.test = test;
        }

        @Override
        public void accept(RecoveryCodeGenerator visitor) {
            visitor.visitBranch(this);
        }

        @Override
        public String toString() {
            return "BRANCH " + address + " " + test;
        }
    }

    /**
     * Unconditionally set the instruction pointer to the specified address.
     */
    private static class Goto extends JumpInstruction {
        Goto(int address) {
            super(address);
        }

        @Override
        public void accept(RecoveryCodeGenerator visitor) {
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
    private static class Load extends Instruction {
        @NotNull final EvaluatorNode expression;

        Load(@NotNull EvaluatorNode expression) {
            this.expression = expression;
        }

        @Override
        public void accept(RecoveryCodeGenerator visitor) {
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
    private static class Return extends Instruction {
        final RecoverySite recoverySite;

        Return(RecoverySite recoverySite) {
            this.recoverySite = recoverySite;
        }

        @Override
        public void accept(RecoveryCodeGenerator visitor) {
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
    private static class Store extends Instruction {
        @NotNull final AbstractVariable variable;
        final RecoverySite recoverySite;

        Store(@NotNull AbstractVariable variable, RecoverySite recoverySite) {
            this.variable = variable;
            this.recoverySite = recoverySite;
        }

        @Override
        public void accept(RecoveryCodeGenerator visitor) {
            visitor.visitStore(this);
        }

        @Override
        public String toString() {
            return "STORE " + variable;
        }
    }

    private static class Copy extends Store {
        Copy(@NotNull AbstractVariable variable, RecoverySite recoverySite) {
            super(variable, recoverySite);
        }

        @Override
        public void accept(RecoveryCodeGenerator visitor) {
            visitor.visitCopy(this);
        }
    }

    /**
     * Translates a tree if {@link EvaluatorNode}s implementing an expression in
     * A-normal form expression into the equivalent A-code.
     */
    static class EvaluatorNodeToACodeTranslator implements EvaluatorNode.Visitor<Void> {
        static Instruction[] translate(EvaluatorNode functionBody) {
            var translator = new EvaluatorNodeToACodeTranslator(functionBody);
            return translator.translate();
        }

        /*
            Instance
         */

        private final EvaluatorNode functionBody;
        private final List<Instruction> code = new ArrayList<>();
        private List<Integer> entryPoints = new ArrayList<>();

        private EvaluatorNodeToACodeTranslator(EvaluatorNode functionBody) {
            this.functionBody = functionBody;
        }

        private Instruction[] translate() {
            functionBody.accept(this); // populates 'code' and 'entryPoints'
            if (entryPoints.isEmpty()) {
                /* Recovery code generation is only expected after generating normal code
                   for a function, and is only requested if the function code included SPE
                   handlers. Thus, the set of entry points should always be non-empty.
                   If that is not so, these assumptions have been violated and something
                   is very wrong. */
                throw new AssertionError();
            }
            emit(new Return(null));
            linkJumps();
            markLiveInstructions();
            removeDeadCode();
            fixJumpAddresses();
            return code.toArray(new Instruction[0]);
        }

        /**
         * Set target fields of jump instructions to point at the instruction at
         * the address of the jump. This is so we can remove dead code by simply
         * deleting elements of the {@link #code} list.
         */
        private void linkJumps() {
            for (var instruction : code) {
                if (instruction instanceof JumpInstruction) {
                    JumpInstruction jump = (JumpInstruction) instruction;
                    jump.target = code.get(jump.address);
                }
            }
        }

        private void markLiveInstructions() {
            var pathways = new ArrayList<>(entryPoints);
            while (!pathways.isEmpty()) {
                var path = pathways.remove(0);
                walkPath(path, pathways);
            }
        }

        private void walkPath(int address, List<Integer> pathways) {
            var instruction = code.get(address);
            while (instruction != null && !instruction.isLive) {
                instruction.isLive = true;
                if (instruction instanceof Goto) {
                    address = ((Goto) instruction).address;
                } else if (instruction instanceof Branch) {
                    pathways.add(((Branch) instruction).address);
                    address++;
                } else {
                    address++;
                }
                instruction = address < code.size() ? code.get(address) : null;
            }
        }

        private void removeDeadCode() {
            code.removeIf(instruction -> !instruction.isLive);
            var redundantGotos = new ArrayList<Instruction>();
            for (int i = 0; i < code.size() - 1; i++) { // not including the last instruction
                var instruction = code.get(i);
                if (instruction instanceof Goto && ((Goto) instruction).target == code.get(i + 1)) {
                    redundantGotos.add(instruction);
                }
            }
            code.removeAll(redundantGotos);
        }

        /**
         * After dead code has been removed, numeric jump addresses become
         * invalid. This is not really a problem because in actual code
         * generation we rely only on direct target pointers established by
         * {@link #linkJumps()}. However, we still fix the addresses to match
         * the new reality as a debugging aid (they are included in the
         * instruction printout).
         */
        private void fixJumpAddresses() {
            var instructionAddresses = new HashMap<Instruction, Integer>();
            for (int i = 0; i < code.size(); i++) {
                instructionAddresses.put(code.get(i), i);
            }
            for (var instruction : code) {
                if (instruction instanceof JumpInstruction) {
                    var jump = (JumpInstruction) instruction;
                    jump.address = instructionAddresses.get(jump.target);
                }
            }
        }

        @Override
        public Void visitCall(CallNode call) {
            emit(new Load(call));
            return null;
        }

        @Override
        public Void visitClosure(ClosureNode closure) {
            emit(new Load(closure));
            return null;
        }

        @Override
        public Void visitConstant(ConstantNode aConst) {
            emit(new Load(aConst));
            return null;
        }

        @Override
        public Void visitFreeFunctionReference(FreeFunctionReferenceNode constFunction) {
            emit(new Load(constFunction));
            return null;
        }

        @Override
        public Void visitIf(IfNode anIf) {
            var branch = new Branch(anIf.condition(), -1);
            emit(branch);
            anIf.falseBranch().accept(this);
            var theGoto = new Goto(-1);
            emit(theGoto);
            branch.address = nextInstructionAddress();
            anIf.trueBranch().accept(this);
            theGoto.address = nextInstructionAddress();
            return null;
        }

        @Override
        public Void visitLet(LetNode let) {
            let.initializer().accept(this);
            entryPoints.add(nextInstructionAddress());
            emit(new Store(let.variable(), let));
            let.body().accept(this);
            return null;
        }

        @Override
        public Void visitPrimitive1(Primitive1Node primitive) {
            emit(new Load(primitive));
            return null;
        }

        @Override
        public Void visitPrimitive2(Primitive2Node primitive) {
            emit(new Load(primitive));
            return null;
        }

        @Override
        public Void visitBlock(BlockNode block) {
            EvaluatorNode[] expressions = block.expressions();
            if (expressions.length == 0) {
                emit(new Load(new ConstantNode(null)));
            } else {
                for (var each : expressions) each.accept(this);
            }
            return null;
        }

        @Override
        public Void visitReturn(ReturnNode ret) {
            ret.value().accept(this);
            entryPoints.add(nextInstructionAddress());
            emit(new Return(ret));
            return null;
        }

        @Override
        public Void visitSetVar(SetVariableNode set) {
            set.value().accept(this);
            entryPoints.add(nextInstructionAddress());
            emit(new Copy(set.variable(), set));
            return null;
        }

        @Override
        public Void visitGetVar(GetVariableNode varRef) {
            emit(new Load(varRef));
            return null;
        }

        private void emit(Instruction instruction) {
            code.add(instruction);
        }

        private int nextInstructionAddress() {
            return code.size();
        }
    }

    private class AtomicExpressionCodeGenerator implements CodeGenerator {
        @Override
        public GhostWriter writer() {
            return writer;
        }

        @Override
        public JvmType generateCode(EvaluatorNode node) {
            return node.accept(this);
        }

        @Override
        public JvmType visitCall(CallNode call) {
            var returnType = call.dispatcher().generateCode(call, this);
            writer.adaptValue(returnType, REFERENCE);
            return REFERENCE;
        }

        @Override
        public MethodType generateArgumentLoad(CallNode callNode) {
            return callNode.match(new CallNode.ArityMatcher<>() {
                @Override
                public MethodType ifNullary() {
                    return MethodType.genericMethodType(0);
                }

                @Override
                public MethodType ifUnary(EvaluatorNode arg) {
                    var argType = generateCode(arg);
                    writer.adaptValue(argType, REFERENCE);
                    return MethodType.genericMethodType(1);
                }

                @Override
                public MethodType ifBinary(EvaluatorNode arg1, EvaluatorNode arg2) {
                    var arg1Type = generateCode(arg1);
                    writer.adaptValue(arg1Type, REFERENCE);
                    var arg2Type = generateCode(arg2);
                    writer.adaptValue(arg2Type, REFERENCE);
                    return MethodType.genericMethodType(2);
                }
            });
        }

        @Override
        public JvmType visitClosure(ClosureNode closure) {
            var indicesToCopy = closure.copiedVariableIndices;
            for (var copiedIndex : indicesToCopy) writer.loadLocal(REFERENCE, copiedIndex);
            writer.invokeDynamic(
                ClosureCreationInvokeDynamic.BOOTSTRAP,
                "createClosure",
                MethodType.genericMethodType(indicesToCopy.length),
                closure.function().id());
            return REFERENCE;
        }

        @Override
        public JvmType visitConstant(ConstantNode aConst) {
            Object value = aConst.value();
            if (value instanceof Integer) {
                writer.loadInt((Integer) value);
                return INT;
            } else if (value instanceof String) {
                writer.loadString((String) value);
                return REFERENCE;
            } else if (value == null) {
                writer.loadNull();
                return REFERENCE;
            } else if (value instanceof Boolean) {
                writer.loadInt((Boolean) value ? 1 : 0);
                return BOOL;
            } else {
                throw new CompilerError("unexpected const value: " + value);
            }
        }

        @Override
        public JvmType visitGetVar(GetVariableNode getVar) {
            var variable = getVar.variable();
            writer.loadLocal(REFERENCE, variable.index());
            if (variable.isBoxed()) writer.extractBoxedVariable();
            return REFERENCE;
        }

        @Override
        public JvmType visitIf(IfNode anIf) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public JvmType visitLet(LetNode let) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public JvmType visitPrimitive1(Primitive1Node primitive1) {
            JvmType argType = primitive1.argument().accept(this);
            return primitive1.implementation().generate(writer, argType);
        }

        @Override
        public JvmType visitPrimitive2(Primitive2Node primitive2) {
            JvmType arg1Type =  primitive2.argument1().accept(this);
            JvmType arg2Type = primitive2.argument2().accept(this);
            return primitive2.implementation().generate(writer, arg1Type, arg2Type);
        }

        @Override
        public JvmType visitBlock(BlockNode block) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public JvmType visitReturn(ReturnNode ret) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public JvmType visitSetVar(SetVariableNode setVar) {
            throw new UnsupportedOperationException("should not be called");
        }

        @Override
        public JvmType visitFreeFunctionReference(FreeFunctionReferenceNode constFunction) {
            return constFunction.generateLoad(writer);
        }
    }


    /*
        Instance
     */

    private final FunctionImplementation function;
    private final Instruction[] acode;
    protected final GhostWriter writer;
    private final AtomicExpressionCodeGenerator atomicGenerator;

    RecoveryCodeGenerator(FunctionImplementation function, GhostWriter writer) {
        this.function = function;
        this.acode = function.recoveryCode();
        assignJumpLabels();
        this.writer = writer;
        this.atomicGenerator = new AtomicExpressionCodeGenerator();
    }

    private void assignJumpLabels() {
        for (var instruction : acode) {
            if (instruction instanceof JumpInstruction) {
                var jump = (JumpInstruction) instruction;
                jump.target.incomingJumpLabel = new Label();
            }
        }
    }

    void generate() {
        for (int i = 0; i < acode.length - 1; i++) { // not including the final return
            var instruction = acode[i];
            if (instruction.incomingJumpLabel != null) {
                writer.setLabelHere(instruction.incomingJumpLabel);
            }
            instruction.accept(this);
        }
        var finalReturn = acode[acode.length - 1];
        if (finalReturn.incomingJumpLabel != null) writer.setLabelHere(finalReturn.incomingJumpLabel);
        writer.bridgeValue(REFERENCE, function.specializedReturnType());
        writer.ret(function.specializedReturnType());
    }

    private void visitBranch(Branch branch) {
        var valueType = branch.test.accept(atomicGenerator);
        writer.adaptValue(valueType, BOOL);
        writer.jumpIfNot0(branch.target.incomingJumpLabel);
    }

    private void visitGoto(Goto aGoto) {
        writer.jump(aGoto.target.incomingJumpLabel);
    }

    private void visitLoad(Load load) {
        var valueType = load.expression.accept(atomicGenerator);
        writer.adaptValue(valueType, REFERENCE);
    }

    private void visitReturn(Return aReturn) {
        setRecoveryLabelHere(aReturn.recoverySite);
        writer.ret(REFERENCE);
    }

    private void visitStore(Store store) {
        setRecoveryLabelHere(store.recoverySite);
        writer.storeLocal(REFERENCE, store.variable.index());
    }

    private void visitCopy(Copy copy) {
        setRecoveryLabelHere(copy.recoverySite);
        writer
            .dup()
            .storeLocal(REFERENCE, copy.variable.index());
    }

    private void setRecoveryLabelHere(@Nullable RecoverySite site) {
        if (site != null) writer.setLabelHere(site.recoverySiteLabel());
    }
}