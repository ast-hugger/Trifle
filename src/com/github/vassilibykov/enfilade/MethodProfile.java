package com.github.vassilibykov.enfilade;

/**
 * Counts method invocations and records observed types of method
 * arguments and other locals.
 */
public class MethodProfile {

    private final Method method;
    private final int methodArity;
    private long invocationCount = 0;
    private final long[] intCaseCounts;
    private final long[] refCaseCounts;

    MethodProfile(Method method) {
        this.method = method;
        this.methodArity = method.arity();
        int frameSize = method.localsCount();
        intCaseCounts = new long[frameSize];
        refCaseCounts = new long[frameSize];
    }

    public long invocationCount() {
        return invocationCount;
    }

    public void recordInvocation(Interpreter.Frame frame) {
        invocationCount++;
        for (int i = 0; i < methodArity; i++) {
            recordArgValue(i, frame.locals[i]);
        }
    }

    private void recordArgValue(int index, Object value) {
        if (value instanceof Integer) {
            intCaseCounts[index]++;
        } else {
            refCaseCounts[index]++;
        }
    }

    public void recordVarStore(Var var, Object value) {
        if (value instanceof Integer) {
            intCaseCounts[var.index()]++;
        } else {
            refCaseCounts[var.index()]++;
        }
    }

    public long intCaseCount(Var var) {
        return intCaseCounts[var.index()];
    }

    public long refCaseCount(Var var) {
        return refCaseCounts[var.index()];
    }

    public boolean isVarProfiled(Var var) {
        int varIndex = var.index();
        return intCaseCounts[varIndex] > 0 || refCaseCounts[varIndex] > 0;
    }

    public boolean isMixed(Var var) {
        int varIndex = var.index();
        if (intCaseCounts[varIndex] == 0 && refCaseCounts[varIndex] == 0) {
            throw new AssertionError("no profile data for var " + var);
        }
        return intCaseCounts[varIndex] > 0 && refCaseCounts[varIndex] > 0;
    }

    public boolean isPureReference(Var var) {
        int varIndex = var.index();
        if (intCaseCounts[varIndex] == 0 && refCaseCounts[varIndex] == 0) {
            throw new AssertionError("no profile data for var " + var);
        }
        return intCaseCounts[varIndex] == 0;
    }

    public boolean isPureInt(Var var) {
        int varIndex = var.index();
        if (intCaseCounts[varIndex] == 0 && refCaseCounts[varIndex] == 0) {
            throw new AssertionError("no profile data for var " + var);
        }
        return refCaseCounts[varIndex] == 0;
    }
}
