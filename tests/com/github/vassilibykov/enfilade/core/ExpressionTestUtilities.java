// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.jetbrains.annotations.TestOnly;

public class ExpressionTestUtilities {

    @TestOnly
    public static void setVariableIndex(Variable var, int index) {
        var.index = index;
    }
}
