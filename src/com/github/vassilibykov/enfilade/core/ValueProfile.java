// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import static com.github.vassilibykov.enfilade.core.TypeCategory.BOOL;
import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;

class ValueProfile {
    private long referenceCases = 0;
    private long intCases = 0;
    private long boolCases = 0;

    public synchronized void recordValue(Object value) {
        if (value instanceof Integer) {
            intCases++;
        } else if (value instanceof Boolean) {
            boolCases++;
        } else {
            referenceCases++;
        }
    }

    public synchronized ExpressionType observedType() {
        if (hasProfileData()) {
            if (referenceCases == 0) {
                if (boolCases == 0) return ExpressionType.known(INT);
                if (intCases == 0) return ExpressionType.known(BOOL);
                // if both are non-0, then the union type is a reference
            }
            return ExpressionType.known(REFERENCE);
        } else {
            return ExpressionType.unknown();
        }
    }

    public synchronized long referenceCases() {
        return referenceCases;
    }

    public synchronized long intCases() {
        return intCases;
    }

    public long boolCases() {
        return boolCases;
    }

    public synchronized TypeCategory valueCategory() {
        return hasProfileData() && isPureInt() ? INT : REFERENCE;
    }

    public synchronized boolean hasProfileData() {
        return referenceCases > 0 || intCases > 0 || boolCases > 0;
    }

    public synchronized boolean isPureInt() {
        if (!hasProfileData()) throw new AssertionError("no profile data");
        return referenceCases == 0 && boolCases == 0;
    }

    public synchronized boolean isPureBool() {
        if (!hasProfileData()) throw new AssertionError("no profile data");
        return referenceCases == 0 && intCases == 0;
    }
}
