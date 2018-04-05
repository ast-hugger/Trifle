// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.primitives.IfAware;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.core.JvmType.VOID;

class CompilerCodeGeneratorSpecialized implements EvaluatorNode.Visitor<JvmType> {
    private final FunctionImplementation function;
    private final GhostWriter writer;
    private final List<AbstractVariable> liveLocals = new ArrayList<>();
    private final List<SquarePegHandler> squarePegHandlers = new ArrayList<>();
    private final JvmType functionReturnType;

    private static class SquarePegHandler {
        private final Label handlerStart;
        private final List<AbstractVariable> liveLocals;
        private final int recoverySiteId;

        private SquarePegHandler(Label handlerStart, List<AbstractVariable> liveLocals, int recoverySiteId) {
            this.handlerStart = handlerStart;
            this.liveLocals = liveLocals;
            this.recoverySiteId = recoverySiteId;
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
//        function.acode = ACodeTranslator.translate(function.body());
        generatePrologue();
        var expressionType = function.body().accept(this);
        bridgeValue(expressionType, functionReturnType);
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

    /*
        An overview of how values produced by visitor methods are handled.

        I believe the following are fundamental invariants. If a method
        doesn't do they say it should, or does something they say it doesn't
        have to, the method is probably wrong.

        An visitor method always returns the JvmType its generated code left on
        the stack.

        A visitor is free to generate a value of any type it wants; it is the
        visitor caller's (the parent expression visitor's) responsibility to
        bridge the value to the type it needs for its own code.

        Bridging may cause SPEs if it's type-narrowing. Thus a visitor method
        which performs bridging must make recovery provisions if it uses the
        value itself. Examples are the visitors of 'let', 'letrec', 'set!'
        and 'return'.

        If a complex subexpression code is generated and bridged in the tail
        position of the visitor code so the visitor does not use it itself,
        like the visitor of 'if' when it bridges each branch value to the
        union type of both branches, there is no need to handle a possible SPE,
        as the expression value is already produced and the SPE handler of
        the complex expression that accepts the value will take care of it.
     */

    @Override
    public JvmType visitCall0(CallNode.Call0 call) {
        var functionType = call.function().accept(this);
        writer.ensureValue(functionType, REFERENCE);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(returnType.representativeClass(), Object.class);
        writer.invokeDynamic(ClosureInvokeDynamic.BOOTSTRAP, "call0", callSiteSignature);
        return returnType;
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
        return returnType;
    }

    private JvmType generateConstantFunctionCall1(CallNode.Call1 call, ConstantFunctionNode function) {
        var argType = call.arg().accept(this);
        var returnType = call.specializationType();
        var callSiteSignature = MethodType.methodType(
            returnType.representativeClass(),
            argType.representativeClass());
        writer.invokeDynamic(ConstantFunctionInvokeDynamic.BOOTSTRAP, "call1", callSiteSignature, function.id());
        return returnType;
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
        return returnType;
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
        var trueBranch = anIf.trueBranch();
        var falseBranch = anIf.falseBranch();
        var resultType = trueBranch.specializationType().union(falseBranch.specializationType());
        // Generate an optimized 'if' form, if possible
        if (anIf.condition() instanceof PrimitiveNode) {
            var primitiveCall = (PrimitiveNode) anIf.condition();
            if (primitiveCall.implementation() instanceof IfAware) {
                var generator = (IfAware) primitiveCall.implementation();
                var maybeOptimized = generator.optimizedFormFor(primitiveCall);
                if (maybeOptimized.isPresent()) {
                    var optimized = maybeOptimized.get();
                    optimized.loadArguments(each -> each.accept(this));
                    writer.withLabelAtEnd(end -> {
                        writer.withLabelAtEnd(elseStart -> {
                            writer.asm().visitJumpInsn(optimized.jumpInstruction(), elseStart);
                            var valueType = trueBranch.accept(this);
                            bridgeValue(valueType, resultType); // in tail position
                            writer.jump(end);
                        });
                        var valueType = falseBranch.accept(this);
                        bridgeValue(valueType, resultType); // in tail position
                    });
                    return resultType;
                }
            }
        }
        // General 'if' form
        var conditionType = anIf.condition().accept(this);
        writer.ensureValue(conditionType, BOOL);
        writer.ifThenElse(
            () -> {
                var valueType = trueBranch.accept(this);
                bridgeValue(valueType, resultType); // in tail position
            },
            () -> {
                var valueType = falseBranch.accept(this);
                bridgeValue(valueType, resultType); // in tail position
            }
        );
        return resultType;
    }

    @Override
    public JvmType visitLet(LetNode let) {
        VariableDefinition variable = let.variable();
        JvmType varType = variable.specializationType();
        withSquarePegRecovery(let, () -> {
            var initType = let.initializer().accept(this);
            bridgeValue(initType, varType);
        });
        if (variable.isBoxed()) {
            writer.initBoxedVariable(varType, variable.index());
        } else {
            writer.storeLocal(varType, variable.index());
        }
        liveLocals.add(variable);
        var bodyType = let.body().accept(this);
        liveLocals.remove(variable);
        return bodyType;
    }

    @Override
    public JvmType visitLetrec(LetrecNode letrec) {
        VariableDefinition variable = letrec.variable();
        JvmType varType = variable.specializationType();
        if (variable.isBoxed()) {
            writer
                .loadDefaultValue(varType)
                .initBoxedVariable(varType, variable.index());
        }
        liveLocals.add(variable);
        withSquarePegRecovery(letrec, () -> {
            var initType = letrec.initializer().accept(this);
            bridgeValue(initType, varType);
        });
        if (variable.isBoxed()) {
            writer.storeBoxedVariable(varType, variable.index());
        } else {
            writer.storeLocal(varType, variable.index());
        }
        var bodyType = letrec.body().accept(this);
        liveLocals.remove(variable);
        return bodyType;
    }

    @Override
    public JvmType visitPrimitive1(Primitive1Node primitive) {
        var argType = primitive.argument().accept(this);
        return primitive.implementation().generate(writer, argType);
    }

    @Override
    public JvmType visitPrimitive2(Primitive2Node primitive) {
        var arg1Type = primitive.argument1().accept(this);
        var arg2Type = primitive.argument2().accept(this);
        return primitive.implementation().generate(writer, arg1Type, arg2Type);
    }

    @Override
    public JvmType visitBlock(BlockNode block) {
        EvaluatorNode[] expressions = block.expressions();
        if (expressions.length == 0) {
            writer.loadNull();
            return REFERENCE;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            expressions[i].accept(this);
            writer.pop();
        }
        return expressions[i].accept(this);
    }

    @Override
    public JvmType visitReturn(ReturnNode returnNode) {
        /* Return causes a non-local control transfer, so the bridging of the return value
           is not in the tail position and must have an SPE handler established.
           The return value is atomic and in principle there is no need to place
           its code within an SPE handler. But just in case we might lift that
           restriction and forget to modify the code here accordingly, we treat
           value() as if it were complex already. */
        withSquarePegRecovery(returnNode, () -> {
            var returnType = returnNode.value().accept(this);
            bridgeValue(returnType, functionReturnType);
        });
        writer.ret(functionReturnType);
        return VOID;
    }

    @Override
    public JvmType visitSetVar(SetVariableNode set) {
        var var = set.variable();
        var varType = var.specializationType();
        withSquarePegRecovery(set, () -> {
            var valueType = set.value().accept(this);
            bridgeValue(valueType, varType);
        });
        writer.dup(); // the duplicate is left on the stack as the set expression value
        if (var.isBoxed()) {
            writer.storeBoxedVariable(varType, var.index());
        } else {
            writer.storeLocal(varType, var.index());
        }
        return varType;
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
            public void ifVoid() {
                // occurs in the middle of blocks and in return statements; nothing needs to be done
            }
        });
    }

    private void withSquarePegRecovery(RecoverySite requestor, Runnable generate) {
        Label handlerStart = new Label();
        SquarePegHandler handler = new SquarePegHandler(
            handlerStart,
            new ArrayList<>(liveLocals),
            requestor.recoverySiteIndex());
        squarePegHandlers.add(handler);
        writer.withLabelsAround((begin, end) -> {
            generate.run();
            writer.handleSquarePegException(begin, end, handlerStart);
        });
    }

    /**
     * Generate code that loads onto the stack the state to be passed to the recovery
     * method. Note that the stack already contains an exception with the value to
     * recover. Once the handler and the epilogue has ran and are ready to call the
     * recovery method, the stack contains (bottom to top):
     * <ul>
     *     <li>SquarePegException exception
     *     <li>int recoverySiteId
     *     <li>Object[] frameReplica
     *     <li>int functionID
     * </ul>
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
        writer.loadInt(handler.recoverySiteId);
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
        handler.liveLocals.forEach(this::storeInFrameReplica);
    }

    private void storeInFrameReplica(AbstractVariable local) {
        JvmType localType = local.isBoxed() ? REFERENCE : local.specializationType();
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
            .invokeStatic(FunctionImplementation.class, "recover",
                Object.class, SquarePegException.class, int.class, Object[].class, int.class);
        bridgeValue(REFERENCE, functionReturnType);
    }
}
