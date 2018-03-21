// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.Nullable;

/**
 * A variable in a program. A variable object must appear exactly once as a
 * function argument, or as a {@link Let}-bound variable. It may appear any
 * number of times in an expression position, evaluating to the current value of
 * the variable. It may also appear any number of times as the {@code variable}
 * of a {@link SetVar} expression.
 *
 * <p>A variable is associated with a storage location in the activation record
 * of its function. The storage location is identified by its index. Indices are
 * assigned by the {@link Function} object when it is constructed. At that time
 * it is detected if a variable does not appear as an argument or a let binding
 * exactly once, as required.
 */
public class Var extends AtomicExpression {
    @Nullable private final String name;
    /*internal*/ int index = -1;
    /*internal*/ final ValueProfile profile = new ValueProfile();

    Var(@Nullable String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public int index() {
        return index;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVar(this);
    }

    @Override
    public String toString() {
        return name != null ? name : "<var" + hashCode() + ">";
    }
}
