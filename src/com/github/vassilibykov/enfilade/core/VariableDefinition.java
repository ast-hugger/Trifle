// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * A variable definition in a function or a let form. Not to be confused with
 * {@link VariableReferenceNode}, which is an atomic expression referencing a variable.
 * Note that a definition is not an expression.
 */
class VariableDefinition {
    private static final ExpressionType KNOWN_VOID = ExpressionType.known(JvmType.VOID);
    private static final ExpressionType UNKNOWN = ExpressionType.unknown();

    @NotNull private final Variable definition;
    @NotNull private final FunctionImplementation hostFunction;
    /*internal*/ int genericIndex = -1;
    /*internal*/ int specializedIndex = -1;
    /*internal*/ final ValueProfile profile = new ValueProfile();
    private ExpressionType inferredType = KNOWN_VOID;
    private ExpressionType observedType = UNKNOWN;

    VariableDefinition(@NotNull Variable definition, FunctionImplementation hostFunction) {
        this.definition = definition;
        this.hostFunction = hostFunction;
    }

    public Variable definition() {
        return definition;
    }

    public boolean isDefinedIn(FunctionImplementation function) {
        return function == hostFunction;
    }

    public FunctionImplementation hostFunction() {
        return hostFunction;
    }

    public String name() {
        return definition.name();
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
     * Return a type the expression should be assumed to produced while
     * generating specialized code. Because specialized code is opportunistic,
     * observed type trumps the inferred type because it's potentially more
     * specific, even if incorrect for the general case.
     */
    public synchronized JvmType specializationType() {
        return observedType.jvmType()
            .orElseGet(() -> inferredType.jvmType()
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
