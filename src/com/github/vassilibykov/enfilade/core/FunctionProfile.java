// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.stream.Stream;

/**
 * Counts function invocations and records observed types of function
 * arguments and other locals.
 */
public class FunctionProfile {

    private final Var[] methodArguments;
    private final int methodArity;
    private long invocationCount = 0;
    private final ValueProfile resultProfile = new ValueProfile();

    FunctionProfile(Function function) {
        this.methodArguments = function.arguments();
        this.methodArity = function.arity();
    }

    public synchronized long invocationCount() {
        return invocationCount;
    }

    public ValueProfile result() {
        return resultProfile;
    }

    public synchronized void recordInvocation(Object[] frame) {
        invocationCount++;
        for (int i = 0; i < methodArity; i++) {
            methodArguments[i].profile.recordValue(frame[i]);
        }
    }

    public void recordResult(Object result) {
        resultProfile.recordValue(result);
    }

    public boolean canBeSpecialized() {
        return Stream.of(methodArguments).anyMatch(some -> some.profile.valueCategory() != TypeCategory.REFERENCE);
    }
}
