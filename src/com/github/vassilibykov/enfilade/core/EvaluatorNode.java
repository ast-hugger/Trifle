// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * A node of a executable representation of the original expression tree. There
 * is a overall resemblance between the type hierarchy of this class and of
 * {@code Expression}, but not a 1:1 correspondence. For example, in executable
 * form expressions are not formally separated into atomic and complex types.
 * Executable form is generated automatically from the original well-formed
 * expression so it is guaranteed to be well-formed without type assurances.
 * Some expression classes may translate into multiple executable forms
 * depending on their properties or position in the tree.
 *
 * @see com.github.vassilibykov.enfilade.expression.Expression
 */
public abstract class EvaluatorNode {

    public interface Visitor<T> {
        T visitBlock(BlockNode block);
        T visitCall0(CallNode.Call0 call);
        T visitCall1(CallNode.Call1 call);
        T visitCall2(CallNode.Call2 call);
        T visitConst(ConstNode aConst);
        T visitIf(IfNode anIf);
        T visitLet(LetNode let);
        T visitPrimitive1(Primitive1Node primitive);
        T visitPrimitive2(Primitive2Node primitive);
        T visitRet(ReturnNode ret);
        T visitVarSet(SetVariableNode set);
        T visitVarRef(VariableReferenceNode varRef);
    }

    public static abstract class VisitorSkeleton<T> implements Visitor<T> {
        @Override
        public T visitBlock(BlockNode block) {
            Stream.of(block.expressions()).forEach(this::visit);
            return null;
        }

        @Override
        public T visitCall0(CallNode.Call0 call) {
            return null;
        }

        @Override
        public T visitCall1(CallNode.Call1 call) {
            call.arg().accept(this);
            return null;
        }

        @Override
        public T visitCall2(CallNode.Call2 call) {
            call.arg1().accept(this);
            call.arg2().accept(this);
            return null;
        }

        @Override
        public T visitConst(ConstNode aConst) {
            return null;
        }

        @Override
        public T visitIf(IfNode anIf) {
            anIf.condition().accept(this);
            anIf.trueBranch().accept(this);
            anIf.falseBranch().accept(this);
            return null;
        }

        @Override
        public T visitLet(LetNode let) {
            let.initializer().accept(this);
            let.body().accept(this);
            return null;
        }

        @Override
        public T visitPrimitive1(Primitive1Node primitive) {
            return null;
        }

        @Override
        public T visitPrimitive2(Primitive2Node primitive) {
            return null;
        }

        @Override
        public T visitRet(ReturnNode ret) {
            return ret.value().accept(this);
        }

        @Override
        public T visitVarSet(SetVariableNode set) {
            set.value().accept(this);
            return null;
        }

        @Override
        public T visitVarRef(VariableReferenceNode var) {
            return null;
        }

        private T visit(EvaluatorNode expr) {
            return expr.accept(this);
        }
    }

    private static final ExpressionType KNOWN_VOID = ExpressionType.known(TypeCategory.VOID);
    private static final ExpressionType UNKNOWN = ExpressionType.unknown();

    /*
        Instance
     */

    private ExpressionType inferredType = KNOWN_VOID;
    private ExpressionType observedType = UNKNOWN;
    private boolean hasBeenEvaluated = false;

    public abstract <T> T accept(Visitor<T> visitor);

    /**
     * Return a type the expression should be assumed to produced while
     * generating specialized code. Because specialized code is opportunistic,
     * observed type trumps the inferred type because it's potentially more
     * specific, even if incorrect for the general case.
     */
    public synchronized TypeCategory specializationType() {
        return observedType.typeCategory()
            .orElseGet(() -> inferredType.typeCategory()
                .orElse(TypeCategory.REFERENCE));
    }

    /*internal*/ synchronized ExpressionType inferredType() {
        return inferredType;
    }

    /*internal*/ synchronized ExpressionType observedType() {
        return observedType;
    }

    /*internal*/ boolean hasBeenEvaluated() {
        return hasBeenEvaluated;
    }

    /*internal*/ synchronized void setInferredType(@NotNull ExpressionType expressionType) {
        this.inferredType = expressionType;
    }

    /*internal*/ synchronized void setObservedType(@NotNull ExpressionType type) {
        observedType = type;
    }

    /*internal*/ void setHasBeenEvaluated(boolean hasBeenEvaluated) {
        this.hasBeenEvaluated = hasBeenEvaluated;
    }

    /**
     * Replace the inferred type of this annotation with the union of the
     * specified inferred type and the current one. Return a boolean indicating
     * whether the unified inferred type is different from the original.
     */
    /*internal*/ synchronized boolean unifyInferredTypeWith(ExpressionType type) {
        ExpressionType newType = inferredType.union(type);
        boolean changed = !inferredType.equals(newType);
        inferredType = newType;
        return changed;
    }

    /*internal*/ synchronized boolean unifyObservedTypeWith(ExpressionType type) {
        ExpressionType newType = observedType.opportunisticUnion(type);
        boolean changed = !observedType.equals(newType);
        observedType = newType;
        return changed;
    }
}
