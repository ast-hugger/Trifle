// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import java.util.List;

/**
 * Counts function invocations and records observed types of function
 * arguments and other locals.
 */
public class FunctionProfile {
    private final List<VariableDefinition> methodParameters;
    private long invocationCount = 0;
    private final ValueProfile resultProfile = new ValueProfile();

    FunctionProfile(List<VariableDefinition> arguments) {
        this.methodParameters = arguments;
    }

    public synchronized long invocationCount() {
        return invocationCount;
    }

    public ValueProfile resultProfile() {
        return resultProfile;
    }

    public synchronized void recordInvocation(Object[] frame) {
        invocationCount++;
        for (var each : methodParameters) {
            each.profile.recordValue(each.getValueIn(frame));
        }
    }

    public void recordResult(Object result) {
        resultProfile.recordValue(result);
    }

    public boolean canBeSpecialized() {
        return methodParameters.stream()
            .anyMatch(some -> some.profile.jvmType() != JvmType.REFERENCE);
    }
}
