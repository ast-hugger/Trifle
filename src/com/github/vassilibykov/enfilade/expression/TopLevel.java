// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.expression;

import com.github.vassilibykov.enfilade.core.Closure;
import com.github.vassilibykov.enfilade.core.FunctionTranslator;

import java.util.function.Function;

/**
 * A trial implementation of a mechanism to allow inexpensive references
 * to top-level functions with recursion. For now, this allows defining
 * only one recursive top-level function.
 */
public class TopLevel {
    public static Closure define(Function<Binding, Lambda> definition) {
        var toplevel = new TopLevel();
        toplevel.defineFunction(definition);
        return toplevel.translate();
    }

    public static class Binding extends AtomicExpression {
        private final Lambda value;
        private Closure closure;

        private Binding(Function<Binding, Lambda> initializer) {
            this.value = initializer.apply(this);
        }

        public Lambda value() {
            return value;
        }

        public Closure closure() {
            return closure;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitTopLevelBinding(this);
        }
    }

    private Binding binding;

    private void defineFunction(Function<Binding, Lambda> initializer) {
        if (binding != null) throw new AssertionError();
        binding = new Binding(initializer);
    }

    private Closure translate() {
        binding.closure = FunctionTranslator.translate(binding.value());
        return binding.closure();
    }
}
