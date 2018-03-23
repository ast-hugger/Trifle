// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

/**
 * Definition, compiler, and interpreter of A-code.
 *
 * <p>A-code is a representation of A-normal forms in a shape resembling a
 * sequence of instructions. It can be executed by A-machine which consists
 * of a vector of local variable values, a current instruction pointer,
 * and a single register (or if you will, a stack of maximum depth 1).
 *
 * <p>There are only seven instructions. Instruction arguments, when present,
 * are actual objects of the specified types.
 *
 * <ul>
 *     <li>load AtomicExpression</li>
 *     <li>call CallExpression</li>
 *     <li>store Variable</li>
 *     <li>drop</li>
 *     <li>branch AtomicExpression int</li>
 *     <li>goto int</li>
 *     <li>return</li>
 * </ul>
 */
package com.github.vassilibykov.enfilade.acode;