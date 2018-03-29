// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * A variable definition in a function or a let form. Not to be confused with
 * {@link GetVariableNode}, which is an atomic expression referencing a variable.
 * Note that a definition is not an expression.
 */
class VariableDefinition extends AbstractVariable {
    private static final ExpressionType KNOWN_VOID = ExpressionType.known(JvmType.VOID);
    private static final ExpressionType UNKNOWN = ExpressionType.unknown();

    /*
        Instance
     */

    @NotNull private final Variable definition;
    private boolean isReferencedNonlocally = false;
    private boolean isMutable = false;
    /*internal*/ final ValueProfile profile = new ValueProfile();
    private ExpressionType inferredType = KNOWN_VOID;
    private ExpressionType observedType = UNKNOWN;

    VariableDefinition(@NotNull Variable definition, FunctionImplementation hostFunction) {
        super(hostFunction);
        this.definition = definition;
    }

    public Variable definition() {
        return definition;
    }

    public String name() {
        return definition.name();
    }

    public boolean isFreeIn(FunctionImplementation function) {
        return function != hostFunction();
    }

    public boolean isReferencedNonlocally() {
        return isReferencedNonlocally;
    }

    public boolean isMutable() {
        return isMutable;
    }

    /*internal*/ void markAsReferencedNonlocally() {
        isReferencedNonlocally = true;
        isBoxed = isMutable;
    }

    /*internal*/ void markAsMutable() {
        isMutable = true;
        isBoxed = isReferencedNonlocally;
    }

    @Override
    ValueProfile profile() {
        return profile;
    }

    @Override
    public synchronized JvmType specializationType() {
        return observedType.jvmType()
            .orElseGet(() -> inferredType.jvmType()
                .orElse(JvmType.REFERENCE));
    }

    /*internal*/ @Override
    synchronized ExpressionType inferredType() {
        return inferredType;
    }

    /*internal*/ @Override
    synchronized ExpressionType observedType() {
        return observedType;
    }

    /*internal*/ @Override
    synchronized void setInferredType(@NotNull ExpressionType expressionType) {
        this.inferredType = expressionType;
    }

    /*internal*/ @Override
    synchronized void setObservedType(@NotNull ExpressionType type) {
        observedType = type;
    }

    /*internal*/ @Override
    synchronized boolean unifyInferredTypeWith(ExpressionType type) {
        ExpressionType newType = inferredType.union(type);
        boolean changed = !inferredType.equals(newType);
        inferredType = newType;
        return changed;
    }

    /*internal*/ @Override
    synchronized boolean unifyObservedTypeWith(ExpressionType type) {
        ExpressionType newType = observedType.opportunisticUnion(type);
        boolean changed = !observedType.equals(newType);
        observedType = newType;
        return changed;
    }

    @Override
    public String toString() {
        return "var(" + name() + ")";
    }
}
