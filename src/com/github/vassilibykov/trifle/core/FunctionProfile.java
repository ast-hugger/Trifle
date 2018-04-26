// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.core;

import java.util.List;

/**
 * Counts function invocations and records observed types of function
 * arguments and other locals.
 */
class FunctionProfile {
    private final List<VariableDefinition> methodParameters;
    private long invocationCount = 0;
    private final ValueProfile resultProfile = new ValueProfile();

    FunctionProfile(List<VariableDefinition> arguments) {
        this.methodParameters = arguments;
    }

    synchronized long invocationCount() {
        return invocationCount;
    }

    ValueProfile resultProfile() {
        return resultProfile;
    }

    synchronized void recordArguments(Object[] frame) {
        for (var each : methodParameters) {
            each.profile.recordValue(each.getValueIn(frame));
        }
    }

    synchronized void recordResult(Object result) {
        /* It's important to count invocations here, after the function returns,
           and not in 'recordArguments' before it's invoked. Counting there may
           in case of recursive calls trigger compilation too early (on the first
           return from a recursive call), when profile data has not yet been
           collected for parts of the function following the recursive return. */
        invocationCount++;
        resultProfile.recordValue(result);
    }
}
