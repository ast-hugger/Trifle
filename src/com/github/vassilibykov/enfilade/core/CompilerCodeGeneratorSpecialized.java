// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.primitives.LessThan;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.core.JvmType.VOID;

class CompilerCodeGeneratorSpecialized implements EvaluatorNode.Visitor<JvmType> {
    private final FunctionImplementation function;
    private final GhostWriter writer;
    private final Deque<JvmType> continuationTypes = new ArrayDeque<>();
    private final List<AbstractVariable> liveLocals = new ArrayList<>();
    private final List<SquarePegHandler> squarePegHandlers = new ArrayList<>();
    private final JvmType functionReturnType;

    private static class SquarePegHandler {
        private final Label handlerStart;
        private final List<AbstractVariable> capturedLocals;
        private final int acodeInitialPC;

        private SquarePegHandler(Label handlerStart, List<AbstractVariable> capturedLocals, int acodeInitialPC) {
            this.handlerStart = handlerStart;
            this.capturedLocals = capturedLocals;
            this.acodeInitialPC = acodeInitialPC;
        }
    }

    /*
        Instance
     */

    CompilerCodeGeneratorSpecialized(FunctionImplementation function, MethodVisitor writer) {
        this.function = function;
        this.functionReturnType = function.body().specializationType();
        this.writer = new GhostWriter(writer);
    }

    public GhostWriter writer() {
        return writer;
    }

    void generate() {
        function.acode = ACodeTranslator.translate(function.body());
        continuationTypes.push(functionReturnType);
        generatePrologue();
        function.body().accept(this);
        continuationTypes.pop();
        writer.ret(functionReturnType);
        if (!squarePegHandlers.isEmpty()) {
            Label epilogue = new Label();
            for (int i = 0; i < squarePegHandlers.size(); i++) {
                boolean isLastHandler = i == squarePegHandlers.size() - 1;
                generateSquarePegHandler(squarePegHandlers.get(i), isLastHandler, epilogue);
            }
            generateEpilogue(epilogue);
        }
    }

    private void generatePrologue() {
        // FIXME: 3/30/18 arguments which are boxed and specialized to primitive types must be relocated on wrapping.
        // If such an argument is copied by a closure, indicesToCopy must reflect the relocated index.
        // This means indicesToCopy must be separate for the specialized case.
        // NO, looks like they shouldn't be. Need to verify and if that's so, the whole
        // generic/specialized index separation is bogus and should be removed.
        for (var each : function.declaredParameters()) {
            if (each.isBoxed()) {
                var type = each.specializationType();
                int index = each.index();
                writer
                    .loadLocal(type, index)
                    .initBoxedVariable(type, index);
            }
        }
    }

    private JvmType currentContinuationType() {
        return continuationTypes.peek();
    }

    @Override
    public JvmType visitCall0(CallNode.Call0 call) {
        generateExpecting(REFERENCE, call.function()); // FIXME: 3/30/18 should not be 'expecting'; a type error is an error
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(returnType.representativeClass(), Object.class);
        writer.invokeDynamic(ClosureInvokeDynamic.BOOTSTRAP, "call0", callSiteSignature);
        assertPassage(returnType, currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitCall1(CallNode.Call1 call) {
        // FIXME 3/25/18 this (and the 2-arg version) will fail if arguments are specialized so the call site
        // has a non-generic signature, but the specialization available in the nexus has a different signature.
        // We'll need to revise the scheme of managing implementations and call sites in FunctionImplementation
        // to fix this.

        if (call.function() instanceof ConstantFunctionNode) {
            return generateConstantFunctionCall1(call, (ConstantFunctionNode) call.function());
        }

        generateExpecting(REFERENCE, call.function()); // FIXME: 3/30/18 should not be 'expecting'; a type error is an error
        var arg = call.arg();
        generateExpecting(arg.specializationType(), arg);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(
            returnType.representativeClass(),
            Object.class, // really a Closure, but we type it as Object to catch errors locally
            arg.specializationType().representativeClass());
        writer.invokeDynamic(ClosureInvokeDynamic.BOOTSTRAP, "call1", callSiteSignature);
        assertPassage(returnType, currentContinuationType());
        return null;
    }

    private JvmType generateConstantFunctionCall1(CallNode.Call1 call, ConstantFunctionNode function) {
        var arg = call.arg();
        generateExpecting(arg.specializationType(), arg);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(
            returnType.representativeClass(),
            arg.specializationType().representativeClass());
        writer.invokeDynamic(ConstantFunctionInvokeDynamic.BOOTSTRAP, "call1", callSiteSignature, function.id());
        assertPassage(returnType, currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitCall2(CallNode.Call2 call) {
        generateExpecting(REFERENCE, call.function()); // FIXME: 3/30/18 should not be 'expecting'; a type error is an error
        var arg1 = call.arg1();
        var arg2 = call.arg2();
        generateExpecting(arg1.specializationType(), arg1);
        generateExpecting(arg2.specializationType(), arg2);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(
            returnType.representativeClass(),
            Object.class, // really a Closure, but we type it as Object to catch errors locally
            arg1.specializationType().representativeClass(),
            arg2.specializationType().representativeClass());
        writer.invokeDynamic(ClosureInvokeDynamic.BOOTSTRAP, "call2", callSiteSignature);
        assertPassage(returnType, currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitClosure(ClosureNode closure) {
        var copiedOuterVariables = closure.copiedOuterVariables;
        writer.newObjectArray(copiedOuterVariables.size());
        for (int i = 0; i < copiedOuterVariables.size(); i++) {
            var variable = copiedOuterVariables.get(i);
            writer
                .dup()
                .loadInt(i);
            if (variable.isBoxed()) {
                writer.loadLocal(REFERENCE, variable.index());
            } else {
                writer
                    .loadLocal(variable.specializationType(), variable.index())
                    .adaptType(variable.specializationType(), REFERENCE);
            }
            writer.asm().visitInsn(Opcodes.AASTORE);
//                .loadLocal(variable.specializationType(), variable.specializedIndex());
//            if (!variable.isBoxed()) {
//                writer.adaptType(variable.specializationType(), REFERENCE);
//            }
        }
        writer
            .loadInt(FunctionRegistry.INSTANCE.lookup(closure.function()))
            .invokeStatic(Closure.class, "create", Closure.class, Object[].class, int.class);
        return REFERENCE;
    }

    @Override
    public JvmType visitConst(ConstNode aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            writer.loadInt((Integer) value);
            assertPassage(INT, currentContinuationType());
        } else if (value instanceof String) {
            writer.loadString((String) value);
            assertPassage(REFERENCE, currentContinuationType());
        } else if (value == null) {
            writer.loadNull();
            assertPassage(REFERENCE, currentContinuationType());
        } else if (value instanceof Boolean) {
            writer.loadInt((Boolean) value ? 1 : 0);
            assertPassage(BOOL, currentContinuationType());
        } else {
            throw new CompilerError("unexpected const value: " + value);
        }
        return null;
    }

    @Override
    public JvmType visitGetVar(GetVariableNode varRef) {
        var variable = varRef.variable();
        var varType = variable.specializationType();
        if (variable.isBoxed()) {
            writer
                .loadLocal(REFERENCE, variable.index())
                .unboxValue(varType);
        } else {
            writer.loadLocal(varType, variable.index());
        }
        assertPassage(varType, currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitIf(IfNode anIf) {
        if (anIf.condition() instanceof LessThan) {
            ((LessThan) anIf.condition()).generateIf(
                (type, arg) -> generateExpecting(type, arg),
                () -> generateForCurrentContinuation(anIf.trueBranch()),
                () -> generateForCurrentContinuation(anIf.falseBranch()),
                writer);
            return null;
        }
        generateExpecting(BOOL, anIf.condition());
        writer.ifThenElse(
            () -> generateForCurrentContinuation(anIf.trueBranch()),
            () -> generateForCurrentContinuation(anIf.falseBranch())
        );
        return null;
    }

    @Override
    public JvmType visitLet(LetNode let) {
        VariableDefinition variable = let.variable();
        JvmType varType = variable.specializationType();
        if (variable.isBoxed() && let.isLetrec()) {
            writer
                .loadDefaultValue(varType)
                .initBoxedVariable(varType, variable.index());
        }
        if (varType == REFERENCE) {
            generateExpecting(REFERENCE, let.initializer());
        } else {
            withSquarePegRecovery(let, () -> generateExpecting(varType, let.initializer()));
        }
        if (variable.isBoxed()) {
            if (let.isLetrec()) {
                writer.storeBoxedVariable(varType, variable.index());
            } else {
                writer.initBoxedVariable(varType, variable.index());
            }
        } else {
            writer.storeLocal(varType, variable.index());
        }
        liveLocals.add(variable);
        generateForCurrentContinuation(let.body());
        liveLocals.remove(variable);
        return null;
    }

    @Override
    public JvmType visitPrimitive1(Primitive1Node primitive) {
        var argType = primitive.argument().specializationType();
        generateExpecting(argType, primitive.argument());
        primitive.generate(writer, argType);
        assertPassage(primitive.jvmType(), currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitPrimitive2(Primitive2Node primitive) {
        var arg1Type = primitive.argument1().specializationType();
        var arg2Type = primitive.argument2().specializationType();
        generateExpecting(arg1Type, primitive.argument1());
        generateExpecting(arg2Type, primitive.argument2());
        primitive.generate(writer, arg1Type, arg2Type);
        assertPassage(primitive.jvmType(), currentContinuationType());
        return null;
    }

    @Override
    public JvmType visitBlock(BlockNode block) {
        EvaluatorNode[] expressions = block.expressions();
        if (expressions.length == 0) {
            writer
                .loadNull()
                .adaptType(REFERENCE, currentContinuationType());
            return null;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            EvaluatorNode expr = expressions[i];
            generateExpecting(VOID, expr);
            writer.pop();
        }
        generateForCurrentContinuation(expressions[i]);
        return null;
    }

    @Override
    public JvmType visitRet(ReturnNode ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public JvmType visitSetVar(SetVariableNode set) {
        var var = set.variable();
        JvmType varType = var.specializationType();
        generateExpecting(varType, set.value());
        writer.dup(); // to leave the value on the stack as the result
        if (var.isBoxed()) {
            writer.storeBoxedVariable(varType, var.index());
        } else {
            writer.storeLocal(varType, var.index());
        }
        return null;
    }

    @Override
    public JvmType visitConstantFunction(ConstantFunctionNode constFunction) {
        var closure = constFunction.closure();
        int id = ConstantFunctionNode.lookup(closure);
        writer
            .loadInt(id)
            .invokeStatic(ConstantFunctionNode.class, "lookup", Closure.class, int.class);
        return REFERENCE;
    }

    private void generateExpecting(JvmType expectedType, EvaluatorNode expression) {
        continuationTypes.push(expectedType);
        expression.accept(this);
        continuationTypes.pop();
    }

    private void generateForCurrentContinuation(EvaluatorNode expression) {
        expression.accept(this);
    }

    /**
     * Assuming that a value of type 'from' is on the stack in the context
     * whose continuation expects a value of type 'to', generate code that will
     * ensure the continuation will successfully receive the value.
     *
     * <p>If the from/to pair of types is such that a value of 'from' cannot in
     * the general case be converted to a value of 'to', for example {@code
     * reference -> int}, the generated code will throw an exception to complete
     * the evaluation in emergency mode.
     *
     * <p>If the 'to' type is VOID, that means the value passed to the continuation
     * will be discarded. In that case it can be anything.
     */
    private void assertPassage(JvmType from, JvmType to) {
        from.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { writer.throwSquarePegException(); }
                    public void ifBoolean() { writer.throwSquarePegException(); }
                    public void ifVoid() { }
                });
            }
            public void ifInt() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { writer.boxInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { writer.boxInteger().throwSquarePegException(); }
                    public void ifVoid() { }
                });
            }
            public void ifBoolean() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { writer.boxBoolean(); }
                    public void ifInt() { writer.boxBoolean().throwSquarePegException(); }
                    public void ifBoolean() { }
                    public void ifVoid() { }
                });
            }
        });
    }

    private void withSquarePegRecovery(LetNode requestor, Runnable generate) {
        Label handlerStart = new Label();
        SquarePegHandler handler = new SquarePegHandler(
            handlerStart,
            new ArrayList<>(liveLocals),
            requestor.setInstructionAddress());
        squarePegHandlers.add(handler);
        writer.withLabelsAround((begin, end) -> {
            generate.run();
            writer.handleSquarePegException(begin, end, handlerStart);
        });
    }

    /**
     * Generate code that loads onto the stack a replica of the frame locals
     * live for the specified handler (an Object[]) and the restart position in
     * A-code, then jumps to the epilogue unless this is the last handler.
     */
    private void generateSquarePegHandler(SquarePegHandler handler, boolean isLastHandler, Label epilogueStart) {
        // TODO: 3/23/18 an optimization opportunity
        // Each handler is currently loading the entire set of its live locals, as
        // generated by generateFrameReplicator(). These sets often have common subsets.
        // For example, each of them includes the function arguments. A smarter approach
        // would be to detect these commonalities and factor them out similarly to how the
        // function epilogue is factored out.
        writer.asm().visitLabel(handler.handlerStart);
        // stack: SquarePegException
        writer.loadInt(handler.acodeInitialPC);
        generateFrameReplicator(handler);
        // stack: SPE, int, Object[]
        if (!isLastHandler) writer.jump(epilogueStart);
    }

    /**
     * Generate a code fragment creating an object array of size equal to the
     * function's locals count and populating it with the values of currently
     * live local variables. The array is left on the stack.
     */
    private void generateFrameReplicator(SquarePegHandler handler) {
        int size = function.frameSize();
        writer.newObjectArray(size);
        handler.capturedLocals.forEach(this::storeInFrameReplica);
    }

    private void storeInFrameReplica(AbstractVariable local) {
        JvmType localType = local.specializationType();
        writer.storeArray(local.index(), () -> {
            writer.loadLocal(localType, local.index());
            writer.adaptType(localType, REFERENCE);
        });
    }

    private void generateEpilogue(Label epilogueStart) {
        writer.asm().visitLabel(epilogueStart);
        // stack: SPE, int initialPC, Object[] frame
        function.declaredParameters().forEach(this::storeInFrameReplica);
        // stack: SPE, int initialPC, Object[] frame
        writer
            .loadInt(FunctionRegistry.INSTANCE.lookup(function))
            // stack: SPE, int initialPC, Object[] frame, int functionId
            .invokeStatic(ACodeInterpreter.class, "forRecovery", ACodeInterpreter.class, int.class, Object[].class, int.class)
            // stack: SPE, Interpreter
            .swap()
            // stack: Interpreter, SPE
            .invokeVirtual(SquarePegException.class, "value", Object.class)
            // stack: Interpreter, Object
            .invokeVirtual(ACodeInterpreter.class, "interpret", Object.class, Object.class);
        functionReturnType.match(new JvmType.VoidMatcher() {
            @Override
            public void ifReference() {
                writer.ret(REFERENCE);
            }

            @Override
            public void ifInt() {
                writer.dup();
                writer.instanceOf(Integer.class);
                writer.ifThenElse(
                    () -> {
                        writer.checkCast(Integer.class);
                        writer.invokeVirtual(Integer.class, "intValue", int.class);
                        writer.ret(INT);
                    },
                    () -> {
                        writer.throwSquarePegException();
                    }
                );
            }

            @Override
            public void ifBoolean() {
                writer.dup();
                writer.instanceOf(Boolean.class);
                writer.ifThenElse(
                    () -> {
                        writer.checkCast(Boolean.class);
                        writer.invokeVirtual(Boolean.class, "booleanValue", boolean.class);
                        writer.ret(BOOL);
                    },
                    () -> {
                        writer.throwSquarePegException();
                    }
                );
            }
        });
    }
}
