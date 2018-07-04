// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.smalltalk.core;

import com.github.vassilibykov.trifle.core.Dictionary;
import com.github.vassilibykov.trifle.expression.Variable;

/**
 * Used during compilation to keep track of the meaning of names
 * available as variables in the current scope.
 */
abstract class Binding {

    interface Matcher<T> {
        T ifGlobal(GlobalBinding globalBinding);
        T ifInstVar(InstVarBinding instVarBinding);
        T ifLocal(LocalBinding localBinding);
    }

    abstract <T> T match(Matcher<T> matcher);

    static class LocalBinding extends Binding {
        private final Variable variable;

        LocalBinding(Variable variable) {
            this.variable = variable;
        }

        Variable variable() {
            return variable;
        }

        @Override
        <T> T match(Matcher<T> matcher) {
            return matcher.ifLocal(this);
        }
    }

    static class InstVarBinding extends Binding {
        private final String name;

        InstVarBinding(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }

        @Override
        <T> T match(Matcher<T> matcher) {
            return matcher.ifInstVar(this);
        }
    }

    static class GlobalBinding extends Binding {
        private final Dictionary.Entry entry;

        GlobalBinding(Dictionary.Entry entry) {
            this.entry = entry;
        }

        Dictionary.Entry entry() {
            return entry;
        }

        @Override
        <T> T match(Matcher<T> matcher) {
            return matcher.ifGlobal(this);
        }
    }
}
