// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.NotNull;

/**
 * A variable reference in a program, not to be confused with {@link VariableDefinition}
 * which is a variable definition. A variable object must appear exactly once as
 * a function argument, or as a {@link LetNode}-bound variable. It may appear any
 * number of times in an expression position, evaluating to the current value of
 * the variable. It may also appear any number of times as the {@code variable}
 * of a {@link SetVariableNode} expression.
 *
 * <p>A variable is associated with a storage location in the activation record
 * of its function. The storage location is identified by its index. Indices are
 * assigned by the {@link FunctionImplementation} object when it is constructed. At that time
 * it is detected if a variable does not appear as an argument or a let binding
 * exactly once, as required.
 */
class VariableReferenceNode extends EvaluatorNode {
    @NotNull /*internal*/ final VariableDefinition variable;

    public VariableReferenceNode(@NotNull VariableDefinition variable) {
        this.variable = variable;
    }

    public VariableDefinition variable() {
        return variable;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVarReference(this);
    }

    public String toString() {
        return variable.toString();
    }
}
