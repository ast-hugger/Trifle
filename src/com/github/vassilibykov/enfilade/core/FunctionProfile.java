// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.List;

/**
 * Counts function invocations and records observed types of function
 * arguments and other locals.
 */
public class FunctionProfile {
    private final List<VariableDefinition> methodArguments;
    private long invocationCount = 0;
    private final ValueProfile resultProfile = new ValueProfile();

    FunctionProfile(List<VariableDefinition> arguments) {
        this.methodArguments = arguments;
    }

    public synchronized long invocationCount() {
        return invocationCount;
    }

    public ValueProfile result() {
        return resultProfile;
    }

    public synchronized void recordInvocation(Object[] frame) {
        invocationCount++;
        for (var each : methodArguments) {
            each.profile.recordValue(frame[each.genericIndex]);
        }
    }

    public void recordResult(Object result) {
        resultProfile.recordValue(result);
    }

    public boolean canBeSpecialized() {
        return methodArguments.stream()
            .anyMatch(some -> some.profile.jvmType() != JvmType.REFERENCE);
    }
}
