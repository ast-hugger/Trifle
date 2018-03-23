// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

/**
 * An expression which, classically, has no side effects, can cause no control
 * transfer, and is guaranteed to terminate producing a result. In our practical
 * terms, the important corollary is that a primitive expression running as
 * compiled JVM code can never encounter an execution failure. Thus, it can safely
 * be compiled with any intermediary results kept on the JVM stack.
 *
 * @author Vassili Bykov
 */
public abstract class AtomicExpression extends Expression {
    /*internal*/ volatile boolean evaluatedWhileProfiling = false;
}
