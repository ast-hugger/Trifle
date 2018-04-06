// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * A synthetic function parameter introduced to receive a copied down value of a free
 * variable. The {@link #original} is the original free variable copied down to this
 * one. The copied variable appears in {@link FunctionImplementation#allParameters}. By the
 * time copied variables are created, the {@link AbstractVariable#isBoxed} status of real
 * variables should already be computed.
 *
 * <p>For type profiling and inferencing, copied variables always delegate to their originals.
 */
class CopiedVariable extends AbstractVariable {
    @NotNull private final VariableDefinition original;
    private AbstractVariable supplier;

    CopiedVariable(@NotNull VariableDefinition original, FunctionImplementation hostFunction) {
        super(hostFunction);
        this.original = original;
        this.isBoxed = original.isBoxed();
    }

    /**
     * The original free variable whose reference was rewritten to refer to this one.
     */
    public VariableDefinition original() {
        return original;
    }

    /**
     * The variable, either defined or copied in the next closure level up
     * from this variable's host, which holds the value copied into this one.
     */
    public AbstractVariable supplier() {
        return supplier;
    }

    public void setSupplier(AbstractVariable supplier) {
        this.supplier = supplier;
    }

    @Override
    ValueProfile profile() {
        return original.profile;
    }

    @Override
    ExpressionType inferredType() {
        return original.inferredType();
    }

    @Override
    JvmType specializedType() {
        return original.specializedType();
    }

    @Override
    void setInferredType(@NotNull ExpressionType expressionType) {
        // nothing to set; the original determines the type
    }

    @Override
    void setSpecializedType(@NotNull JvmType type) {
        // nothing to set; the original determines the type
    }

    @Override
    boolean unifyInferredTypeWith(ExpressionType type) {
        return original.unifyInferredTypeWith(type);
    }

    @Override
    void setupArgumentIn(Object[] frame, Object value) {
        // a synthetic parameter; any passed in value is already boxed and must be copied directly
        frame[index] = value;
    }

    @Override
    public String toString() {
        return "copied " + original.toString();
    }
}
