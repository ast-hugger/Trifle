// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

/**
 * Definition, compiler, and interpreter of A-code.
 *
 * <p>A-code is a representation of A-normal forms in a shape resembling a
 * sequence of instructions. It can be executed by A-machine which consists
 * of a vector of local variable values, a current instruction pointer,
 * and a stack of maximum depth 1.
 *
 * <p>The instruction set includes the following seven. Instruction arguments,
 * when present, are understood to be actual objects of the specified types.
 *
 * <ul>
 *     <li>load AtomicExpression</li>
 *     <li>call CallExpression</li>
 *     <li>store Variable</li>
 *     <li>drop</li>
 *     <li>if Label</li>
 *     <li>goto Label</li>
 *     <li>return</li>
 * </ul>
 */
package com.github.vassilibykov.enfilade.acode;