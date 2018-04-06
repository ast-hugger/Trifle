// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.primitives;

import com.github.vassilibykov.enfilade.core.EvaluatorNode;
import com.github.vassilibykov.enfilade.core.PrimitiveNode;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A primitive implementation class implements this interface if it's a
 * boolean-valued primitive, and for some combinations of argument types it can
 * compile an {@code if} expression using a JVM instruction performing the test
 * and a conditional jump as a single operation. For example, an {@code if}
 * expression whose test is the {@link LessThan} primitive can be implemented as
 * {@code IF_ICMPGE} if the arguments are {@code ints}.
 */
public interface IfAware {

    interface OptimizedIfForm {
        void loadArguments(Consumer<EvaluatorNode> argumentGenerator);
        int  jumpInstruction();
    }

    /**
     * Check if this primitive can produce an optimized form of {@code if}
     * for the given call. If it can, return a helper object that will
     * assist the compiler with generating the optimized form.
     */
    Optional<OptimizedIfForm> optimizedFormFor(PrimitiveNode ifCondition);
}
