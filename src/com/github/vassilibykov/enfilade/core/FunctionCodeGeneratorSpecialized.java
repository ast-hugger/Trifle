// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.primitives.LessThan;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.github.vassilibykov.enfilade.core.TypeCategory.BOOL;
import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;

class FunctionCodeGeneratorSpecialized implements Expression.Visitor<TypeCategory> {
    private final GhostWriter writer;
    private final Deque<TypeCategory> continuationTypes = new ArrayDeque<>();

    FunctionCodeGeneratorSpecialized(MethodVisitor writer) {
        this.writer = new GhostWriter(writer);
    }

    public GhostWriter writer() {
        return writer;
    }

    TypeCategory generate(Function function) {
        TypeCategory returnType = function.body().compilerAnnotation.specializationType();
        continuationTypes.push(returnType);
        function.body().accept(this);
        continuationTypes.pop();
        return returnType;
    }

    private TypeCategory currentContinuationType() {
        return continuationTypes.peek();
    }

    @Override
    public TypeCategory visitCall0(Call0 call) {
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        TypeCategory returnType = call.compilerAnnotation.specializationType();
        MethodType callSiteType = MethodType.methodType(returnType.representativeClass());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        assertPassage(returnType, currentContinuationType());
        return null;
    }

    @Override
    public TypeCategory visitCall1(Call1 call) {
        // FIXME this (and the 2-arg version) will fail if arguments are specialized so the call site
        // has a non-generic signature, but the specialization available in the nexus has a different signature.
        // We'll need to revise the scheme of managing implementations and call sites in Nexus
        // to fix this.
        call.arg().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        TypeCategory returnType = call.compilerAnnotation.specializationType();
        MethodType callSiteType = MethodType.methodType(
            returnType.representativeClass(),
            call.arg().compilerAnnotation.specializationType().representativeClass());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        assertPassage(returnType, currentContinuationType());
        return null;
    }

    @Override
    public TypeCategory visitCall2(Call2 call) {
        call.arg1().accept(this);
        call.arg2().accept(this);
        int id = FunctionRegistry.INSTANCE.lookup(call.function());
        TypeCategory returnType = call.compilerAnnotation.specializationType();
        MethodType callSiteType = MethodType.methodType(
            returnType.representativeClass(),
            call.arg1().compilerAnnotation.specializationType().representativeClass(),
            call.arg2().compilerAnnotation.specializationType().representativeClass());
        writer.invokeDynamic(
            DirectCall.BOOTSTRAP,
            "call#" + id,
            callSiteType,
            id);
        assertPassage(returnType, currentContinuationType());
        return null;
    }

    @Override
    public TypeCategory visitConst(Const aConst) {
        Object value = aConst.value();
        if (value instanceof Integer) {
            writer.loadInt((Integer) value);
            assertPassage(INT, currentContinuationType());
        } else if (value instanceof String) {
            writer.loadString((String) value);
            assertPassage(REFERENCE, currentContinuationType());
        } else {
            throw new CompilerError("unexpected const value: " + value);
        }
        return null;
    }

    @Override
    public TypeCategory visitIf(If anIf) {
        if (anIf.condition() instanceof LessThan) {
            ((LessThan) anIf.condition()).generateIf(
                (type, arg) -> generateExpecting(type, arg),
                () -> generateForCurrentContinuation(anIf.trueBranch()),
                () -> generateForCurrentContinuation(anIf.falseBranch()),
                writer);
            return null;
        }
        generateExpecting(BOOL, anIf.condition());
        writer.withLabelAtEnd(end -> {
            writer.withLabelAtEnd(elseStart -> {
                writer.jumpIf0(elseStart);
                generateForCurrentContinuation(anIf.trueBranch());
                writer.jump(end);
            });
            generateForCurrentContinuation(anIf.falseBranch());
        });
        return null;
    }

    @Override
    public TypeCategory visitLet(Let let) {
        Variable var = let.variable();
        TypeCategory varType = var.compilerAnnotation.specializationType();
        generateExpecting(varType, let.initializer());
        writer.storeLocal(varType, var.index());
        generateForCurrentContinuation(let.body());
        return null;
    }

    @Override
    public TypeCategory visitPrimitive1(Primitive1 primitive) {
        primitive.argument().accept(this);
        primitive.generate(writer, primitive.argument().compilerAnnotation.specializationType());
        assertPassage(primitive.valueCategory(), currentContinuationType());
        return null;
    }

    @Override
    public TypeCategory visitPrimitive2(Primitive2 primitive) {
        primitive.argument1().accept(this);
        primitive.argument2().accept(this);
        primitive.generate(
            writer,
            primitive.argument1().compilerAnnotation.specializationType(),
            primitive.argument2().compilerAnnotation.specializationType());
        assertPassage(primitive.valueCategory(), currentContinuationType());
        return null;
    }

    @Override
    public TypeCategory visitProg(Prog prog) {
        Expression[] expressions = prog.expressions();
        if (expressions.length == 0) {
            writer
                .loadNull()
                .adaptType(REFERENCE, currentContinuationType());
            return null;
        }
        int i;
        for (i = 0; i < expressions.length - 1; i++) {
            Expression expr = expressions[i];
            generateExpecting(expr.compilerAnnotation.specializationType(), expr);
            writer.pop();
        }
        generateForCurrentContinuation(expressions[i]);
        return null;
    }

    @Override
    public TypeCategory visitRet(Ret ret) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO implement
    }

    @Override
    public TypeCategory visitVarSet(VarSet set) {
        Variable var = set.variable;
        TypeCategory varType = var.compilerAnnotation.specializationType();
        generateExpecting(varType, set.value());
        writer
            .dup()
            .storeLocal(varType, var.index());
        return null;
    }

    @Override
    public TypeCategory visitVarRef(VarRef varRef) {
        TypeCategory varType = varRef.variable.compilerAnnotation.specializationType();
        writer.loadLocal(varType, varRef.variable.index());
        assertPassage(varType, currentContinuationType());
        return null;
    }

    private void generateExpecting(TypeCategory expectedType, Expression expression) {
        continuationTypes.push(expectedType);
        expression.accept(this);
        continuationTypes.pop();
    }

    private void generateForCurrentContinuation(Expression expression) {
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
     */
    private void assertPassage(TypeCategory from, TypeCategory to) {
        from.match(new TypeCategory.VoidMatcher() {
            public void ifReference() {
                to.match(new TypeCategory.VoidMatcher() {
                    public void ifReference() { }
                    public void ifInt() { writer.throwSquarePegException(); }
                    public void ifBoolean() { writer.throwSquarePegException(); }
                });
            }
            public void ifInt() {
                to.match(new TypeCategory.VoidMatcher() {
                    public void ifReference() { writer.boxInteger(); }
                    public void ifInt() { }
                    public void ifBoolean() { writer.boxInteger().throwSquarePegException(); }
                });
            }
            public void ifBoolean() {
                to.match(new TypeCategory.VoidMatcher() {
                    public void ifReference() { writer.boxBoolean(); }
                    public void ifInt() { writer.boxBoolean().throwSquarePegException(); }
                    public void ifBoolean() { }
                });
            }
        });
    }
}
