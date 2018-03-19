package com.github.vassilibykov.enfilade;

/**
 * Counts function invocations and records observed types of function
 * arguments and other locals.
 */
public class FunctionProfile {

    private final Function function;
    private final Var[] methodArguments;
    private final int methodArity;
    private long invocationCount = 0;

    FunctionProfile(Function function) {
        this.function = function;
        this.methodArguments = function.arguments();
        this.methodArity = function.arity();
    }

    public long invocationCount() {
        return invocationCount;
    }

    public void recordInvocation(Object[] frame) {
        invocationCount++;
        for (int i = 0; i < methodArity; i++) {
            methodArguments[i].profile.recordValue(frame[i]);
        }
    }
}
