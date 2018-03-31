// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

abstract class AbstractVariable {
    @NotNull protected final FunctionImplementation hostFunction;
    protected boolean isBoxed = false;
    /*internal*/ int genericIndex = -1;
    /*internal*/ int specializedIndex = -1;

    AbstractVariable(@NotNull FunctionImplementation hostFunction) {
        this.hostFunction = hostFunction;
    }

    /**
     * The index assigned to this variable which is safe to use in generic code,
     * where two variables may reuse the same local slot if they are not live at
     * the same time. In specialized code we must segregate variables by their
     * type.
     */
    public int genericIndex() {
        return genericIndex;
    }

    /**
     * The index assigned to this variable by specialized code generator.
     */
    public int specializedIndex() {
        return specializedIndex;
    }

    /**
     * The function in which the variable is defined, as an argument or as a let-bound
     * variable. {@link #genericIndex()} and {@link #specializedIndex()} are meaningful
     * only in the frame of this function. If the variable is copied, the host function
     * is the function in which it is a copied argument.
     */
    public FunctionImplementation hostFunction() {
        return hostFunction;
    }

    /**
     * Indicate whether this variable value should be boxed in the frame. This is
     * required for mutable variables with non-local references.
     */
    public boolean isBoxed() {
        return isBoxed;
    }

    /**
     * The type profile of the variable. For a copied variable, this is the type
     * profile of the original.
     */
    abstract ValueProfile profile();

    /**
     * The type the expression should be assumed to produced while
     * generating specialized code. Because specialized code is opportunistic,
     * observed type trumps the inferred type because it's potentially more
     * specific, even if incorrect for the general case.
     */
    abstract JvmType specializationType();

    /*internal*/ abstract ExpressionType inferredType();

    /*internal*/ abstract ExpressionType observedType();

    /*internal*/ abstract void setInferredType(@NotNull ExpressionType expressionType);

    /*internal*/ abstract void setObservedType(@NotNull ExpressionType type);

    /**
     * Replace the inferred type of this annotation with the union of the
     * specified inferred type and the current one. Return a boolean indicating
     * whether the unified inferred type is different from the original.
     */
    /*internal*/ abstract boolean unifyInferredTypeWith(ExpressionType type);

    /**
     * Replace the observed type of this annotation with the (lower bound) union
     * of the specified type and the current one. Return a boolean indicating whether
     * the unified type is different from the original.
     */
    /*internal*/ abstract boolean unifyObservedTypeWith(ExpressionType type);

    void initValueIn(Object[] frame, Object value) {
        // It's tempting to get rid of the 'if' by adding 'boxed' subclasses of variables
        // and polymorphically separating the boxed and unboxed logic that way. However,
        // that complicates matters by requiring an extra rewrite of the evaluator tree
        // and brings no performance benefits because call sites of this and
        // the other two value access methods become polymorphic. In essence, the 'if'
        // below is traded for the 'if' in the call site cache dispatch.
        if (isBoxed) {
            frame[genericIndex] = BoxedReference.with(value);
        } else {
            frame[genericIndex] = value;
        }
    }

    Object getValueIn(Object[] frame) {
        return isBoxed ? ((BoxedReference) frame[genericIndex]).value : frame[genericIndex];
    }

    void setValueIn(Object[] frame, Object value) {
        if (isBoxed) {
            ((BoxedReference) frame[genericIndex]).value = value;
        } else {
            frame[genericIndex] = value;
        }
    }

    abstract void setupArgumentIn(Object[] frame, Object value);
}
