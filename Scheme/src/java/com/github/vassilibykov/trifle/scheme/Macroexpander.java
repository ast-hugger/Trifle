// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import com.github.vassilibykov.trifle.core.Invocable;

import java.util.Map;

class Macroexpander {

    private final Map<String, Invocable> macroexpanders;

    Macroexpander(Map<String, Invocable> macroexpanders) {
        this.macroexpanders = macroexpanders;
    }

    Object expand(Object original) {
        return expandObject(original);
    }

    private Object expandObject(Object original) {
        return original instanceof Pair ? expandForm((Pair) original) : original;
    }

    private Object expandForm(Pair head) {
        var car = head.car();
        if (car instanceof Symbol) {
            var name = ((Symbol) car).name();
            var expander = macroexpanders.get(name);
            if (expander != null) {
                var expanded = expander.invoke(head);
                return expanded instanceof Pair ? expandList((Pair) expanded) : expanded;
            }
        }
        return expandList(head);
    }

    private Object expandList(Pair list) {
        if (list == null) return null;
        var head = list.car();
        var expandedHead = expandObject(head);
        var tail = list.cdr();
        var expandedTail = expandObject(tail);
        return head == expandedHead && tail == expandedTail ? list : Pair.of(expandedHead, expandedTail);
    }
}
