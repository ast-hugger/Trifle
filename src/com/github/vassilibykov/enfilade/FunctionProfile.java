package com.github.vassilibykov.enfilade;

import java.util.stream.Stream;

/**
 * Counts function invocations and records observed types of function
 * arguments and other locals.
 */
public class FunctionProfile {

    private final Var[] methodArguments;
    private final int methodArity;
    private long invocationCount = 0;

    FunctionProfile(Function function) {
        this.methodArguments = function.arguments();
        this.methodArity = function.arity();
    }

    public synchronized long invocationCount() {
        return invocationCount;
    }

    public synchronized void recordInvocation(Object[] frame) {
        invocationCount++;
        for (int i = 0; i < methodArity; i++) {
            methodArguments[i].profile.recordValue(frame[i]);
        }
    }

    public boolean canBeSpecialized() {
        return Stream.of(methodArguments).anyMatch(some -> some.profile.valueCategory() != TypeCategory.REFERENCE);
    }
}
