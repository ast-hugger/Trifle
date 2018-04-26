// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.expression;

/**
 * A complex expression is an expression evaluation of which may fail to
 * terminate normally. Because of the structure imposed on expressions by ANF
 * constraints, a complex expression can always be compiled so it begins
 * evaluation with an empty JVM method frame stack. This is a critically
 * important property because it guarantees that if compiled code execution
 * fails and the framework needs to recover by completing the current frame in
 * interpreted mode, any intermediary computation state can be transferred to
 * the interpreter.
 *
 * @author Vassili Bykov
 */
public abstract class ComplexExpression extends Expression {
}
