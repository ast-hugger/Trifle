// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * A variable definition in a function or a let form. Not to be confused with
 * {@link VariableReferenceNode}, which is an atomic expression referencing a variable.
 * Note that a definition is not an expression.
 */
public class VariableDefinition {
    private static final ExpressionType KNOWN_VOID = ExpressionType.known(JvmType.VOID);
    private static final ExpressionType UNKNOWN = ExpressionType.unknown();

    @NotNull private final Variable definition;
    /*internal*/ int index = -1;
    /*internal*/ final ValueProfile profile = new ValueProfile();
    private ExpressionType inferredType = KNOWN_VOID;
    private ExpressionType observedType = UNKNOWN;

    VariableDefinition(@NotNull Variable definition) {
        this.definition = definition;
    }

    public Variable definition() {
        return definition;
    }

    public String name() {
        return definition.name();
    }

    public int index() {
        return index;
    }

    /**
     * Return a type the expression should be assumed to produced while
     * generating specialized code. Because specialized code is opportunistic,
     * observed type trumps the inferred type because it's potentially more
     * specific, even if incorrect for the general case.
     */
    public synchronized JvmType specializationType() {
        return observedType.typeCategory()
            .orElseGet(() -> inferredType.typeCategory()
                .orElse(JvmType.REFERENCE));
    }

    /*internal*/ synchronized ExpressionType inferredType() {
        return inferredType;
    }

    /*internal*/ synchronized ExpressionType observedType() {
        return observedType;
    }

    /*internal*/ synchronized void setInferredType(@NotNull ExpressionType expressionType) {
        this.inferredType = expressionType;
    }

    /*internal*/ synchronized void setObservedType(@NotNull ExpressionType type) {
        observedType = type;
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

    @Override
    public String toString() {
        return name();
    }
}
