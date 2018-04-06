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

    /*
        Instance
     */

    @NotNull private final Variable definition;
    private boolean isReferencedNonlocally = false;
    private boolean isMutable = false;
    /*internal*/ final ValueProfile profile = new ValueProfile();
    private ExpressionType inferredType = KNOWN_VOID;
    private JvmType specializedType = JvmType.VOID;

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

    /*internal*/ @Override
    synchronized ExpressionType inferredType() {
        return inferredType;
    }

    /*internal*/ @Override
    synchronized JvmType specializedType() {
        return specializedType;
    }

    /*internal*/ @Override
    synchronized void setInferredType(@NotNull ExpressionType expressionType) {
        this.inferredType = expressionType;
    }

    /*internal*/ @Override
    synchronized void setSpecializedType(@NotNull JvmType type) {
        specializedType = type;
    }

    /*internal*/ @Override
    synchronized boolean unifyInferredTypeWith(ExpressionType type) {
        ExpressionType newType = inferredType.union(type);
        boolean changed = !inferredType.equals(newType);
        inferredType = newType;
        return changed;
    }

    @Override
    void setupArgumentIn(Object[] frame, Object value) {
        // a declared parameter; must be boxed if needed
        initValueIn(frame, value);
    }

    @Override
    public String toString() {
        return "var(" + name() + ")";
    }
}
