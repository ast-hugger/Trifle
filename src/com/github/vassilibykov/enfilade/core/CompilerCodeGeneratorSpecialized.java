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
        generatePrologue();
        generateForContinuationType(functionReturnType, function.body());
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
        for (var each : function.declaredParameters()) {
            if (each.isBoxed()) {
                var paramType = each.specializationType();
                int index = each.index();
                writer
                    .loadLocal(paramType, index)
                    .initBoxedVariable(paramType, index);
            }
        }
    }

    private JvmType currentContinuationType() {
        return continuationTypes.peek();
    }

    /*
        Invariants in the visit methods:

        An atomic expression visitor returns the type of the value produced.
        It's not supposed to need any of the 'generateFor...' methods, or something
        is not right.

        A complex expression visitor does not need to return a meaningful value.
        I'm returning null so things break if assumptions are wrong.
        Instead, it needs to do 'bridgeValue' before returning
     */

    @Override
    public JvmType visitCall0(CallNode.Call0 call) {
        var functionType = call.function().accept(this);
        writer.ensureValue(functionType, REFERENCE);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(returnType.representativeClass(), Object.class);
        writer.invokeDynamic(ClosureInvokeDynamic.BOOTSTRAP, "call0", callSiteSignature);
        bridgeToCurrentContinuationType(returnType);
        return null;
    }

    @Override
    public JvmType visitCall1(CallNode.Call1 call) {
        if (call.function() instanceof ConstantFunctionNode) {
            return generateConstantFunctionCall1(call, (ConstantFunctionNode) call.function());
        }
        var functionType = call.function().accept(this);
        writer.ensureValue(functionType, REFERENCE);
        var argType = call.arg().accept(this);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(
            returnType.representativeClass(),
            Object.class, // the closure being called
            argType.representativeClass());
        writer.invokeDynamic(ClosureInvokeDynamic.BOOTSTRAP, "call1", callSiteSignature);
        bridgeToCurrentContinuationType(returnType);
        return null;
    }

    private JvmType generateConstantFunctionCall1(CallNode.Call1 call, ConstantFunctionNode function) {
        var argType = call.arg().accept(this);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(
            returnType.representativeClass(),
            argType.representativeClass());
        writer.invokeDynamic(ConstantFunctionInvokeDynamic.BOOTSTRAP, "call1", callSiteSignature, function.id());
        bridgeToCurrentContinuationType(returnType);
        return null;
    }

    @Override
    public JvmType visitCall2(CallNode.Call2 call) {
        var functionType = call.function().accept(this);
        writer.ensureValue(functionType, REFERENCE);
        var arg1Type = call.arg1().specializationType();
        var arg2Type = call.arg2().specializationType();
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(
            returnType.representativeClass(),
            Object.class, // really a Closure, but we type it as Object to catch errors locally
            arg1Type.representativeClass(),
            arg2Type.representativeClass());
        writer.invokeDynamic(ClosureInvokeDynamic.BOOTSTRAP, "call2", callSiteSignature);
        bridgeToCurrentContinuationType(returnType);
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
                JvmType variableType = variable.specializationType();
                writer
                    .loadLocal(variableType, variable.index())
                    .adaptValue(variableType, REFERENCE);
            }
            writer.asm().visitInsn(Opcodes.AASTORE);
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
        } else if (value instanceof String) {
            writer.loadString((String) value);
        } else if (value == null) {
            writer.loadNull();
        } else if (value instanceof Boolean) {
            writer.loadInt((Boolean) value ? 1 : 0);
        } else {
            throw new CompilerError("unexpected const value: " + value);
        }
        return aConst.specializationType();
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
        return varType;
    }

    @Override
    public JvmType visitIf(IfNode anIf) {
        if (anIf.condition() instanceof LessThan) {
            ((LessThan) anIf.condition()).generateIf(
                (type, arg) -> {
                    var argType = arg.accept(this);
                    writer.ensureValue(argType, type);
                },
                () -> generateForCurrentContinuation(anIf.trueBranch()),
                () -> generateForCurrentContinuation(anIf.falseBranch()),
                writer);
            return null;
        }
        var conditionType = anIf.condition().accept(this);
        writer.ensureValue(conditionType, BOOL);
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
            generateForContinuationType(REFERENCE, let.initializer());
        } else {
            withSquarePegRecovery(let, () -> generateForContinuationType(varType, let.initializer()));
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
        var argType = primitive.argument().accept(this); // argument is a primitive
        primitive.generate(writer, argType);
        return primitive.jvmType();
    }

    @Override
    public JvmType visitPrimitive2(Primitive2Node primitive) {
        var arg1Type = primitive.argument1().accept(this);
        var arg2Type = primitive.argument2().accept(this);
        primitive.generate(writer, arg1Type, arg2Type);
        return primitive.jvmType();
    }

    @Override
    public JvmType visitBlock(BlockNode block) {
        EvaluatorNode[] expressions = block.expressions();
        if (expressions.length == 0) {
            writer
                .loadNull()
                .adaptValue(REFERENCE, currentContinuationType());
            return REFERENCE;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            EvaluatorNode expr = expressions[i];
            generateForContinuationType(VOID, expr);
            writer.pop();
        }
        generateForCurrentContinuation(expressions[i]);
        return null;
    }

    @Override
    public JvmType visitReturn(ReturnNode returnNode) {
        var valueType = returnNode.value().accept(this);
        bridgeValue(valueType, functionReturnType);
        writer.ret(functionReturnType);
        return null;
    }

    @Override
    public JvmType visitSetVar(SetVariableNode set) {
        var var = set.variable();
        var varType = var.specializationType();
        if (varType == REFERENCE) {
            generateForContinuationType(REFERENCE, set.value());
        } else {
            withSquarePegRecovery(set, () -> generateForContinuationType(varType, set.value()));
        }
        writer.dup(); // to leave the value on the stack as the result
        if (var.isBoxed()) {
            writer.storeBoxedVariable(varType, var.index());
        } else {
            writer.storeLocal(varType, var.index());
        }
        bridgeToCurrentContinuationType(set.specializationType());
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

    private void generateForContinuationType(JvmType expectedType, EvaluatorNode expression) {
        continuationTypes.push(expectedType);
        expression.accept(this);
        continuationTypes.pop();
    }

    private void generateForCurrentContinuation(EvaluatorNode expression) {
        expression.accept(this);
    }

    /**
     * Assuming that a value of type 'from' is on the stack in the context whose
     * continuation expects a value of type 'to', generate code that will ensure
     * the continuation will successfully receive the value.
     *
     * <p>If the from/to pair of types is such that a value of 'from' cannot in
     * the general case be converted to a value of 'to', for example {@code
     * reference -> int}, the generated code will throw an exception to complete
     * the evaluation in emergency mode.
     *
     * <p>If the 'to' type is VOID, that means the value will be discarded by
     * the continuation, so it doesn't matter what it is.
     *
     * <p>This is different from {@link GhostWriter#adaptValue(JvmType,
     * JvmType)}. The latter performs wrapping and unwrapping of values,
     * assuming that in a conversion between a primitive and a reference type,
     * the reference type is a valid wrapper value for the primitive. In a
     * conversion from a reference to an int, the reference can value never be
     * anything other than {@code Integer}. This is true no matter if the user
     * program is correct or not. A violation of this expectation is a sign of
     * an internal error in the compiler.
     *
     * <p>In contrast, in bridging a reference to an int it's normal for the
     * reference value to not be an {@code Integer}. In that case it should be
     * packaged up and thrown as a {@link SquarePegException}.
     *
     */
    private void bridgeValue(JvmType from, JvmType to) {
        from.match(new JvmType.VoidMatcher() {
            public void ifReference() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { writer.unwrapIntegerOr(writer::throwSquarePegException); }
                    public void ifBoolean() { writer.unwrapBooleanOr(writer::throwSquarePegException); }
                    public void ifVoid() { }
                });
            }
            public void ifInt() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { writer.wrapInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { writer.wrapInteger().throwSquarePegException(); }
                    public void ifVoid() { }
                });
            }
            public void ifBoolean() {
                to.match(new JvmType.VoidMatcher() {
                    public void ifReference() { writer.wrapBoolean(); }
                    public void ifInt() { writer.wrapBoolean().throwSquarePegException(); }
                    public void ifBoolean() { }
                    public void ifVoid() { }
                });
            }
        });
    }

    private void bridgeToCurrentContinuationType(JvmType from) {
        bridgeValue(from, currentContinuationType());
    }

    private void withSquarePegRecovery(RecoverySite requestor, Runnable generate) {
        Label handlerStart = new Label();
        SquarePegHandler handler = new SquarePegHandler(
            handlerStart,
            new ArrayList<>(liveLocals),
            requestor.resumptionAddress());
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
            writer.adaptValue(localType, REFERENCE);
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
        bridgeValue(REFERENCE, functionReturnType);
    }
}
