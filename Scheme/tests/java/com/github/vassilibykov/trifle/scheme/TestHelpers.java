// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import java.util.Objects;

class TestHelpers {

    /**
     * Construct a Scheme list out of an array of elements,
     * converting nested arrays into nested lists and strings
     * into symbols.
     */
    static Pair listify(Object... elements) {
        Pair result = null;
        for (int i = elements.length - 1; i >= 0; i--) {
            var element = elements[i];
            if (element instanceof Object[]) {
                element = listify((Object[]) element);
            } else if (element instanceof String) {
                element = Symbol.named((String) element);
            }
            result = Pair.of(element, result);
        }
        return result;
    }

    static boolean deeplyMatch(Object a, Object b) {
        if (a instanceof Pair) {
            return b instanceof Pair
                && deeplyMatch(((Pair) a).car(), ((Pair) b).car())
                && deeplyMatch(((Pair) a).cdr(), ((Pair) b).cdr());
        } else {
            return Objects.equals(a, b);
        }
    }
}
