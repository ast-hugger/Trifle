// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.expression.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * A special kind of a variable definition used for declared function parameters.
 * It has no special behavior per se, but having a dedicated class for parameters
 * allows the compiler to sometimes treat them specially.
 */
class ParameterDefinition extends VariableDefinition {
    ParameterDefinition(@NotNull Variable definition, FunctionImplementation hostFunction) {
        super(definition, hostFunction);
    }
}
