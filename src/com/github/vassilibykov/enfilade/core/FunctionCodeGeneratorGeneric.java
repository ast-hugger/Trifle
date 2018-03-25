// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;

import static com.github.vassilibykov.enfilade.core.TypeCategory.BOOL;
import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;

/**
 * A code generator producing code for the generic version of a function.
 * The result returned by each visitor method is the type category of the
 * value of the the subexpression compiled by the method.
 */
class FunctionCodeGeneratorGeneric implements EvaluatorNode.Visitor<TypeCategory> {
    protected final GhostWriter writer;

    FunctionCodeGeneratorGeneric(MethodVisitor visitor) {
        this.writer = new GhostWriter(visitor);
    }

    @Override
    public TypeCategory visitCall0(CallNode.Call0 call) {
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        MethodType callSiteType = MethodType.methodType(Object.class);
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        return REFERENCE;
    }

    @Override
    public TypeCategory visitCall1(CallNode.Call1 call) {
        TypeCategory argType = call.arg().accept(this);
        writer.adaptType(argType, REFERENCE);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        MethodType callSiteType = MethodType.methodType(Object.class, Object.class);
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        return REFERENCE;
    }

    @Override
    public TypeCategory visitCall2(CallNode.Call2 call) {
        TypeCategory arg1Type = call.arg1().accept(this);
        writer.adaptType(arg1Type, REFERENCE);
        TypeCategory arg2Type = call.arg2().accept(this);
        writer.adaptType(arg2Type, REFERENCE);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        MethodType callSiteType = MethodType.methodType(Object.class, Object.class, Object.class);
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        return REFERENCE;
    }

    @Override
    public TypeCategory visitConst(ConstNode aConst) {
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
    public TypeCategory visitIf(IfNode anIf) {
        TypeCategory testType = anIf.condition().accept(this);
        writer.adaptType(testType, BOOL);
        writer.ifThenElse(
            () -> {
                TypeCategory type = anIf.trueBranch().accept(this);
                writer.adaptType(type, REFERENCE);
            },
            () -> {
                TypeCategory type = anIf.falseBranch().accept(this);
                writer.adaptType(type, REFERENCE);
            }
        );
        return REFERENCE;
    }

    @Override
    public TypeCategory visitLet(LetNode let) {
        TypeCategory initType = let.initializer().accept(this);
        writer.adaptType(initType, REFERENCE);
        writer.storeLocal(REFERENCE, let.variable().index());
        return let.body().accept(this);
    }

    @Override
    public TypeCategory visitPrimitive1(Primitive1Node primitive1) {
        TypeCategory argType = primitive1.argument().accept(this);
        return primitive1.generate(writer, argType);
    }

    @Override
    public TypeCategory visitPrimitive2(Primitive2Node primitive2) {
        TypeCategory arg1Type =  primitive2.argument1().accept(this);
        TypeCategory arg2Type = primitive2.argument2().accept(this);
        return primitive2.generate(writer, arg1Type, arg2Type);
    }

    @Override
    public TypeCategory visitBlock(BlockNode block) {
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
    public TypeCategory visitRet(ReturnNode ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public TypeCategory visitVarSet(SetVariableNode set) {
        TypeCategory varType = set.value().accept(this);
        writer
            .adaptType(varType, REFERENCE)
            .dup()
            .storeLocal(REFERENCE, set.variable.index());
        return REFERENCE;
    }

    @Override
    public TypeCategory visitVarRef(VariableReferenceNode var) {
        writer.loadLocal(REFERENCE, var.variable.index());
        return REFERENCE;
    }
}
