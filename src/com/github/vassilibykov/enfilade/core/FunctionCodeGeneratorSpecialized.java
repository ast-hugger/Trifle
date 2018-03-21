// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.primitives.LessThan;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.github.vassilibykov.enfilade.core.TypeCategory.BOOLEAN;

class FunctionCodeGeneratorSpecialized extends FunctionCodeGeneratorGeneric {
    private final Deque<TypeCategory> continuationTypes = new ArrayDeque<>();

    FunctionCodeGeneratorSpecialized(MethodVisitor writer) {
        super(writer);
    }

    TypeCategory generate(Function function) {
        TypeCategory observedReturnType = function.profile.result().valueCategory();
        continuationTypes.push(observedReturnType);
        function.body().accept(this);
        continuationTypes.pop();
        return observedReturnType;
    }

    private TypeCategory currentContinuationType() {
        return continuationTypes.peek();
    }

    @Override
    public TypeCategory visitCall0(Call0 call) {
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        TypeCategory observedReturnType = call.compilerAnnotation.valueCategory();
        MethodType callSiteType = MethodType.methodType(observedReturnType.representativeType());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        return observedReturnType;
    }

    @Override
    public TypeCategory visitCall1(Call1 call) {
        // FIXME this (and the 2-arg version) will fail if arguments are specialized so the call site
        // has a non-generic signature, but the specialization available in the nexus has a different signature.
        // We'll need to revise the scheme of managing implementations and call sites in Nexus
        // to fix this.
        TypeCategory arg1Type = call.arg().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        TypeCategory observedReturnType = call.compilerAnnotation.valueCategory();
        MethodType callSiteType = MethodType.methodType(observedReturnType.representativeType(), arg1Type.representativeType());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        return observedReturnType;
    }

    @Override
    public TypeCategory visitCall2(Call2 call) {
        TypeCategory arg1Type = call.arg1().accept(this);
        TypeCategory arg2Type = call.arg2().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        TypeCategory observedReturnType = call.compilerAnnotation.valueCategory();
        MethodType callSiteType = MethodType.methodType(
            observedReturnType.representativeType(),
            arg1Type.representativeType(),
            arg2Type.representativeType());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        return observedReturnType;
    }

    @Override
    public TypeCategory visitIf(If theIf) {
        if (theIf.condition() instanceof LessThan) {
            return ((LessThan) theIf.condition()).generateIf(theIf, this, writer);
        }
        TypeCategory testType = theIf.condition().accept(this);
        writer.adaptType(testType, BOOLEAN);
        TypeCategory[] branchTypes = new TypeCategory[2];
        writer.withLabelAtEnd(end -> {
            writer.withLabelAtEnd(elseStart -> {
                writer.jumpIf0(elseStart);
                branchTypes[0] = theIf.trueBranch().accept(this);
                writer.jump(end);
            });
            branchTypes[1] = theIf.falseBranch().accept(this);
        });
        return branchTypes[0].union(branchTypes[1]);
    }

    @Override
    public TypeCategory visitLet(Let let) {
        Var var = let.variable();
        TypeCategory varType = var.compilerAnnotation.valueCategory();
        TypeCategory initType = let.initializer().accept(this);
        writer
            .adaptType(initType, varType)
            .storeLocal(varType, var.index());
        return let.body().accept(this);
    }

    @Override
    public TypeCategory visitRet(Ret ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public TypeCategory visitSetVar(SetVar set) {
        Var var = set.variable();
        TypeCategory varType = var.compilerAnnotation.valueCategory();
        TypeCategory valueType = set.value().accept(this);
        writer
            .adaptType(valueType, varType)
            .dup()
            .storeLocal(varType, var.index());
        return varType;
    }

    @Override
    public TypeCategory visitVar(Var var) {
        TypeCategory varType = var.compilerAnnotation.valueCategory();
        writer.loadLocal(varType, var.index());
        return varType;
    }
}
