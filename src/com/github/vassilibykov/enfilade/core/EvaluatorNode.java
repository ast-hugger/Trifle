// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * A node of an executable representation of the original expression tree. There
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

    interface Visitor<T> {
        T visitBlock(BlockNode block);
        T visitCall(CallNode call);
        T visitClosure(ClosureNode closure);
        T visitConstant(ConstantNode aConst);
        T visitFreeFunctionReference(FreeFunctionReferenceNode constFunction);
        T visitGetVar(GetVariableNode varRef);
        T visitIf(IfNode anIf);
        T visitLet(LetNode let);
        T visitPrimitive1(Primitive1Node primitive);
        T visitPrimitive2(Primitive2Node primitive);
        T visitReturn(ReturnNode ret);
        T visitSetVar(SetVariableNode setVar);
    }

    static abstract class VisitorSkeleton<T> implements Visitor<T> {
        @Override
        public T visitBlock(BlockNode block) {
            Stream.of(block.expressions()).forEach(this::visit);
            return null;
        }

        @Override
        public T visitCall(CallNode call) {
            call.dispatcher().evaluatorNode().ifPresent(it -> it.accept(this));
            call.match(new CallNode.ArityMatcher<Void>() {
                @Override
                public Void ifNullary() {
                    return null;
                }

                @Override
                public Void ifUnary(EvaluatorNode arg) {
                    arg.accept(VisitorSkeleton.this);
                    return null;
                }

                @Override
                public Void ifBinary(EvaluatorNode arg1, EvaluatorNode arg2) {
                    arg1.accept(VisitorSkeleton.this);
                    arg2.accept(VisitorSkeleton.this);
                    return null;
                }
            });
            return null;
        }

        @Override
        public T visitConstant(ConstantNode aConst) {
            return null;
        }

        @Override
        public T visitClosure(ClosureNode closure) {
            closure.function().body().accept(this);
            return null;
        }

        @Override
        public T visitGetVar(GetVariableNode var) {
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
            primitive.argument().accept(this);
            return null;
        }

        @Override
        public T visitPrimitive2(Primitive2Node primitive) {
            primitive.argument1().accept(this);
            primitive.argument2().accept(this);
            return null;
        }

        @Override
        public T visitReturn(ReturnNode ret) {
            return ret.value().accept(this);
        }

        @Override
        public T visitSetVar(SetVariableNode set) {
            set.value().accept(this);
            return null;
        }

        @Override
        public T visitFreeFunctionReference(FreeFunctionReferenceNode topLevelBinding) {
            return null;
        }

        private T visit(EvaluatorNode expr) {
            return expr.accept(this);
        }
    }

    private static final ExpressionType KNOWN_VOID = ExpressionType.known(JvmType.VOID);

    /*
        Instance
     */

    private ExpressionType inferredType = KNOWN_VOID;
    private JvmType specializedType = null; // should always get replaced by something meaningful

    public abstract <T> T accept(Visitor<T> visitor);

    /*internal*/ ExpressionType inferredType() {
        return inferredType;
    }

    /*internal*/ void setInferredType(@NotNull ExpressionType expressionType) {
        this.inferredType = expressionType;
    }

    public JvmType specializedType() {
        return specializedType;
    }

    /*internal*/ void setSpecializedType(@NotNull JvmType type) {
        specializedType = type;
    }

    /**
     * Replace the inferred type of this annotation with the union of the
     * specified inferred type and the current one. Return a boolean indicating
     * whether the unified inferred type is different from the original.
     */
    /*internal*/ boolean unifyInferredTypeWith(ExpressionType type) {
        ExpressionType newType = inferredType.union(type);
        boolean changed = !inferredType.equals(newType);
        inferredType = newType;
        return changed;
    }
}
