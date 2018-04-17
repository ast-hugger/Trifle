// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.objectweb.asm.Label;

/**
 * Implemented by {@link EvaluatorNode}s which may require switching execution
 * to recovery mode when their compiled representation is running.
 */
interface RecoverySite {
    Label recoverySiteLabel();
    void setRecoverySiteLabel(Label recoverySiteLabel);
}
