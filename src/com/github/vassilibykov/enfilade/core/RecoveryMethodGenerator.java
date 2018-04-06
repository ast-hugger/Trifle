// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static com.github.vassilibykov.enfilade.core.JvmType.VOID;

/**
 * A code generator producing code for the recovery method of a function.
 */
class RecoveryMethodGenerator implements EvaluatorNode.Visitor<JvmType> {
    private final static int VALUE_ARG = 0;
    private final static int RECOVERY_ADDRESS_ARG = 1;
    private final static int FRAME_ARG = 2;
    private final static int LOCAL_VAR_BASE = 3;

    private final FunctionImplementation function;
    protected final GhostWriter writer;
    private Map<RecoverySite, Label> recoverySiteLabels = new HashMap<>();

    RecoveryMethodGenerator(FunctionImplementation function, MethodVisitor visitor) {
        this.function = function;
        this.writer = new GhostWriter(visitor);
    }

    JvmType generate() {
        generatePrologue(function);
        return function.body().accept(this);
    }

    private void generatePrologue(FunctionImplementation function) {
        writer.loadLocal(REFERENCE, FRAME_ARG);
        for (int i = 0; i < function.frameSize(); i++) {
            writer
                .dup()
                .loadInt(i);
            writer.asm().visitInsn(Opcodes.AALOAD);
            writer.storeLocal(REFERENCE, i + LOCAL_VAR_BASE);
        }
        writer
            .pop()
            .loadLocal(REFERENCE, VALUE_ARG)
            .loadLocal(INT, RECOVERY_ADDRESS_ARG);
        var recoverySites = function.recoverySites();
        var labels = new Label[recoverySites.size()];
        for (int i = 0; i < recoverySites.size(); i++) {
            RecoverySite site = recoverySites.get(i);
            var label = new Label();
            recoverySiteLabels.put(site, label);
            labels[i] = label;
        }
        writer.withLabelAtEnd(methodStart ->
            writer.asm().visitTableSwitchInsn(0, recoverySites.size() - 1, methodStart, labels));
        writer.pop();
    }

    @Override
    public JvmType visitCall0(CallNode.Call0 call) {
        if (call.function() instanceof FunctionConstantNode) {
            return generateConstantFunctionCall0(call, (FunctionConstantNode) call.function());
        }
        call.function().accept(this); // puts a value on the stack that must be a closure
        var type = MethodType.genericMethodType(1); // Closure is the argument
        writer.invokeDynamic(
            ClosureInvokeDynamic.BOOTSTRAP,
            "call0",
            type);
        return REFERENCE;
    }

    private JvmType generateConstantFunctionCall0(CallNode.Call0 call, FunctionConstantNode constFunction) {
        // For a constant call, the closure is not pushed on the stack.
        // Its ID is instead encoded in the invokedynamic instruction.
        var type = MethodType.genericMethodType(0);
        writer.invokeDynamic(
            ConstantFunctionInvokeDynamic.BOOTSTRAP,
            "call0",
            type,
            constFunction.id());
        return REFERENCE;
    }

    @Override
    public JvmType visitCall1(CallNode.Call1 call) {
        if (call.function() instanceof FunctionConstantNode) {
            return generateConstantFunctionCall1(call, (FunctionConstantNode) call.function());
        }
        call.function().accept(this); // puts a value on the stack which must be a closure
        var argType = call.arg().accept(this);
        var type = MethodType.genericMethodType(2);
        writer.adaptValue(argType, REFERENCE);
        writer.invokeDynamic(
            ClosureInvokeDynamic.BOOTSTRAP,
            "call1",
            type);
        return REFERENCE;
    }

    private JvmType generateConstantFunctionCall1(CallNode.Call1 call, FunctionConstantNode constFunction) {
        var type = MethodType.genericMethodType(1);
        var argType = call.arg().accept(this);
        writer.adaptValue(argType, REFERENCE);
        writer.invokeDynamic(
            ConstantFunctionInvokeDynamic.BOOTSTRAP,
            "call1",
            type,
            constFunction.id());
        return REFERENCE;
    }

    @Override
    public JvmType visitCall2(CallNode.Call2 call) {
        if (call.function() instanceof FunctionConstantNode) {
            return generateConstantFunctionCall2(call, (FunctionConstantNode) call.function());
        }
        call.function().accept(this); // puts a value on the stack that must be a closure
        var arg1Type = call.arg1().accept(this);
        var type = MethodType.genericMethodType(3);
        writer.adaptValue(arg1Type, REFERENCE);
        var arg2Type = call.arg2().accept(this);
        writer.adaptValue(arg2Type, REFERENCE);
        writer.invokeDynamic(
            ClosureInvokeDynamic.BOOTSTRAP,
            "call2",
            type);
        return REFERENCE;
    }

    private JvmType generateConstantFunctionCall2(CallNode.Call2 call, FunctionConstantNode constFunction) {
        var type = MethodType.genericMethodType(1);
        var arg1Type = call.arg1().accept(this);
        writer.adaptValue(arg1Type, REFERENCE);
        var arg2Type = call.arg2().accept(this);
        writer.adaptValue(arg2Type, REFERENCE);
        writer.invokeDynamic(
            ConstantFunctionInvokeDynamic.BOOTSTRAP,
            "call1",
            type,
            constFunction.id());
        return REFERENCE;
    }

    @Override
    public JvmType visitClosure(ClosureNode closure) {
        var indicesToCopy = closure.copiedVariableIndices;
        writer.newObjectArray(indicesToCopy.length);
        for (int i = 0; i < indicesToCopy.length; i++) {
            writer
                .dup()
                .loadInt(i)
                .loadLocal(REFERENCE, indicesToCopy[i] + LOCAL_VAR_BASE);
            writer.asm().visitInsn(Opcodes.AASTORE);
        }
        writer
            .loadInt(FunctionRegistry.INSTANCE.lookup(closure.function()))
            .invokeStatic(Closure.class, "create", Closure.class, Object[].class, int.class);
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
        writer.loadLocal(REFERENCE, variable.index() + LOCAL_VAR_BASE);
        if (variable.isBoxed()) writer.extractBoxedVariable();
        return REFERENCE;
    }

    @Override
    public JvmType visitIf(IfNode anIf) {
        JvmType testType = anIf.condition().accept(this);
        writer.adaptValue(testType, BOOL);
        writer.ifThenElse(
            () -> {
                JvmType type = anIf.trueBranch().accept(this);
                writer.adaptValue(type, REFERENCE);
            },
            () -> {
                JvmType type = anIf.falseBranch().accept(this);
                writer.adaptValue(type, REFERENCE);
            }
        );
        return REFERENCE;
    }

    @Override
    public JvmType visitLet(LetNode let) {
        var variable = let.variable();
        var initType = let.initializer().accept(this);
        writer.adaptValue(initType, REFERENCE);
        setRecoveryLabelHere(let);
        if (variable.isBoxed()) {
            writer.initBoxedReference(variable.index() + LOCAL_VAR_BASE);
        } else {
            writer.storeLocal(REFERENCE, variable.index() + LOCAL_VAR_BASE);
        }
        return let.body().accept(this);
    }

    @Override
    public JvmType visitLetrec(LetrecNode letrec) {
        var variable = letrec.variable();
        if (variable.isBoxed()) {
            writer
                .loadNull()
                .initBoxedReference(variable.index() + LOCAL_VAR_BASE);
        }
        var initType = letrec.initializer().accept(this);
        writer.adaptValue(initType, REFERENCE);
        setRecoveryLabelHere(letrec);
        if (variable.isBoxed()) {
            writer.storeBoxedReference(variable.index() + LOCAL_VAR_BASE);
        } else {
            writer.storeLocal(REFERENCE, variable.index() + LOCAL_VAR_BASE);
        }
        return letrec.body().accept(this);
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
        EvaluatorNode[] expressions = block.expressions();
        if (expressions.length == 0) {
            writer.loadNull();
            return REFERENCE;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            EvaluatorNode expr = expressions[i];
            expr.accept(this);
            writer.pop();
        }
        return expressions[i].accept(this);
    }

    @Override
    public JvmType visitReturn(ReturnNode ret) {
        var valueType = ret.accept(this);
        writer.adaptValue(valueType, REFERENCE);
        setRecoveryLabelHere(ret);
        writer.ret(valueType);
        return VOID;
    }

    @Override
    public JvmType visitSetVar(SetVariableNode setVar) {
        var variable = setVar.variable();
        var varType = setVar.value().accept(this);
        writer.adaptValue(varType, REFERENCE);
        setRecoveryLabelHere(setVar);
        writer.dup();
        if (variable.isBoxed()) {
            writer.storeBoxedReference(variable.index() + LOCAL_VAR_BASE);
        } else {
            writer.storeLocal(REFERENCE, variable.index() + LOCAL_VAR_BASE);
        }
        return REFERENCE;
    }

    @Override
    public JvmType visitConstantFunction(FunctionConstantNode constFunction) {
        int id = constFunction.id();
        writer
            .loadInt(id)
            .invokeStatic(FunctionRegistry.class, "findAsClosure", Closure.class, int.class);
        return REFERENCE;
    }

    private void setRecoveryLabelHere(RecoverySite site) {
        writer.asm().visitLabel(recoverySiteLabels.get(site));
    }
}
