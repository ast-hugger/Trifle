// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

/**
 * Nodes of an expression tree accepted by Enfilade as input.
 *
 * <p>Expression structure is modeled after A-normal forms, with any specific
 * expression classified as either atomic or complex. Complex expressions are
 * not allowed as subexpression of certain expressions. For example, an argument
 * of a function or a primitive call must be atomic. Because function calls are
 * complex, a function call may not appear as an argument of another function
 * call. In contrast, a primitive call is atomic so an argument of a primitive
 * call may be another primitive call. (So atomicity should not be confused with
 * being a terminal of the expression grammar).
 *
 * <p>This structure (statically verifiable by the type system!) of the input
 * language has beneficial properties for addressing some of the key issues
 * in adaptively translating these expressions into the JVM code.
 */
package com.github.vassilibykov.enfilade.expression;