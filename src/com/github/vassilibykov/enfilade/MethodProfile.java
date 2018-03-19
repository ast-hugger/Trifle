package com.github.vassilibykov.enfilade;

/**
 * Counts method invocations and records observed types of method
 * arguments and other locals.
 */
public class MethodProfile {

    private final Method method;
    private final Var[] methodArguments;
    private final int methodArity;
    private long invocationCount = 0;

    MethodProfile(Method method) {
        this.method = method;
        this.methodArguments = method.arguments();
        this.methodArity = method.arity();
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
