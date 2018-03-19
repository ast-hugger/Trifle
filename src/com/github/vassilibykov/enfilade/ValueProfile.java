package com.github.vassilibykov.enfilade;

import static com.github.vassilibykov.enfilade.ValueCategory.INT;
import static com.github.vassilibykov.enfilade.ValueCategory.REFERENCE;

class ValueProfile {
    private long referenceCases = 0;
    private long intCases = 0;

    public void recordValue(Object value) {
        if (value instanceof Integer) {
            intCases++;
        } else {
            referenceCases++;
        }
    }

    public long referenceCases() {
        return referenceCases;
    }

    public long intCases() {
        return intCases;
    }

    public ValueCategory valueCategory() {
        return hasProfileData() && isPureInt() ? INT : REFERENCE;
    }

    public boolean hasProfileData() {
        return referenceCases > 0 || intCases > 0;
    }

    public boolean isPureInt() {
        if (!hasProfileData()) {
            throw new AssertionError("no profile data");
        }
        return referenceCases == 0;
    }
}
