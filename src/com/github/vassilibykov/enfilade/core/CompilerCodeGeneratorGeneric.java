// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;

import static com.github.vassilibykov.enfilade.core.JvmType.BOOL;
import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;

/**
 * A code generator producing code for the generic version of a function.
 * The result returned by each visitor method is the type category of the
 * value of the the subexpression compiled by the method.
 */
class CompilerCodeGeneratorGeneric implements EvaluatorNode.Visitor<JvmType> {
    protected final GhostWriter writer;

    CompilerCodeGeneratorGeneric(MethodVisitor visitor) {
        this.writer = new GhostWriter(visitor);
    }

    @Override
    public JvmType visitCall0(CallNode.Call0 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
//        int id = Environment.INSTANCE.lookup(call.function());
//        MethodType callSiteType = MethodType.methodType(Object.class);
//        writer.invokeDynamic(
//            DirectCall.BOOTSTRAP,
//            "call#" + id,
//            callSiteType,
//            id);
//        return REFERENCE;
    }

    @Override
    public JvmType visitCall1(CallNode.Call1 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
//        JvmType argType = call.arg().accept(this);
//        writer.adaptType(argType, REFERENCE);
//        int id = Environment.INSTANCE.lookup(call.function().implementation);
//        MethodType callSiteType = MethodType.methodType(Object.class, Object.class);
//        writer.invokeDynamic(
//            DirectCall.BOOTSTRAP,
//            "call#" + id,
//            callSiteType,
//            id);
//        return REFERENCE;
    }

    @Override
    public JvmType visitCall2(CallNode.Call2 call) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
//        JvmType arg1Type = call.arg1().accept(this);
//        writer.adaptType(arg1Type, REFERENCE);
//        JvmType arg2Type = call.arg2().accept(this);
//        writer.adaptType(arg2Type, REFERENCE);
//        int id = Environment.INSTANCE.lookup(call.function().implementation);
//        MethodType callSiteType = MethodType.methodType(Object.class, Object.class, Object.class);
//        writer.invokeDynamic(
//            DirectCall.BOOTSTRAP,
//            "call#" + id,
//            callSiteType,
//            id);
//        return REFERENCE;
    }

    @Override
    public JvmType visitClosure(ClosureNode closure) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public JvmType visitConst(ConstNode aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            writer.loadInt((Integer) value);
            return INT;
        } else if (value instanceof String) {
            writer.loadString((String) value);
            return REFERENCE;
        } else {
            throw new CompilerError("unexpected const value: " + value);
        }
    }

    @Override
    public JvmType visitFreeVarReference(FreeVariableReferenceNode varRef) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public JvmType visitIf(IfNode anIf) {
        JvmType testType = anIf.condition().accept(this);
        writer.adaptType(testType, BOOL);
        writer.ifThenElse(
            () -> {
                JvmType type = anIf.trueBranch().accept(this);
                writer.adaptType(type, REFERENCE);
            },
            () -> {
                JvmType type = anIf.falseBranch().accept(this);
                writer.adaptType(type, REFERENCE);
            }
        );
        return REFERENCE;
    }

    @Override
    public JvmType visitLet(LetNode let) {
        JvmType initType = let.initializer().accept(this);
        writer.adaptType(initType, REFERENCE);
        writer.storeLocal(REFERENCE, let.variable().genericIndex());
        return let.body().accept(this);
    }

    @Override
    public JvmType visitPrimitive1(Primitive1Node primitive1) {
        JvmType argType = primitive1.argument().accept(this);
        return primitive1.generate(writer, argType);
    }

    @Override
    public JvmType visitPrimitive2(Primitive2Node primitive2) {
        JvmType arg1Type =  primitive2.argument1().accept(this);
        JvmType arg2Type = primitive2.argument2().accept(this);
        return primitive2.generate(writer, arg1Type, arg2Type);
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
    public JvmType visitRet(ReturnNode ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public JvmType visitSetFreeVar(SetFreeVariableNode setFreeVariableNode) {
        return null;
    }

    @Override
    public JvmType visitSetVar(SetVariableNode set) {
        JvmType varType = set.value().accept(this);
        writer
            .adaptType(varType, REFERENCE)
            .dup()
            .storeLocal(REFERENCE, set.variable.genericIndex());
        return REFERENCE;
    }

    @Override
    public JvmType visitVarReference(VariableReferenceNode var) {
        writer.loadLocal(REFERENCE, var.variable.genericIndex());
        return REFERENCE;
    }
}
