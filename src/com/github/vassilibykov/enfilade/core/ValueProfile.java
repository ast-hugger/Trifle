package com.github.vassilibykov.enfilade.core;

import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;

class ValueProfile {
    private long referenceCases = 0;
    private long intCases = 0;

    public synchronized void recordValue(Object value) {
        if (value instanceof Integer) {
            intCases++;
        } else {
            referenceCases++;
        }
    }

    public synchronized long referenceCases() {
        return referenceCases;
    }

    public synchronized long intCases() {
        return intCases;
    }

    public synchronized TypeCategory valueCategory() {
        return hasProfileData() && isPureInt() ? INT : REFERENCE;
    }

    public synchronized boolean hasProfileData() {
        return referenceCases > 0 || intCases > 0;
    }

    public synchronized boolean isPureInt() {
        if (!hasProfileData()) {
            throw new AssertionError("no profile data");
        }
        return referenceCases == 0;
    }
}
