// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Helpers {
    private Helpers() {}

    static Object car(Object mustBePair) {
        return ((Pair) mustBePair).car();
    }

    static Object cdr(Object mustBePair) {
        return ((Pair) mustBePair).cdr();
    }

    static Object caar(Object mustBePair) {
        return car(car(mustBePair));
    }

    static Object cadr(Object mustBePair) {
        return car(cdr(mustBePair));
    }

    static Object cdar(Object mustBePair) {
        return cdr(car(mustBePair));
    }

    static Object cddr(Object mustBePair) {
        return cdr(cdr(mustBePair));
    }

    static Object caadr(Object mustBePair) {
        return car(car(cdr(mustBePair)));
    }

    static Object cadar(Object mustBePair) {
        return car(cdr(car(mustBePair)));
    }

    static Object cdadr(Object mustBePair) {
        return cdr(car(cdr(mustBePair)));
    }

    static Object caddr(Object mustBePair) {
        return car(cdr(cdr(mustBePair)));
    }

    static Object cdddr(Object mustBePair) {
        return cdr(cdr(cdr(mustBePair)));
    }

    static Object caaddr(Object mustBePair) {
        return car(car(cdr(cdr(mustBePair))));
    }

    static Object cadadr(Object mustBePair) {
        return car(cdr(car(cdr(mustBePair))));
    }

    static Object cadddr(Object mustBePair) {
        return car(cdr(cdr(cdr(mustBePair))));
    }

    static List<String> asListOfSymbolNames(Object listOfSymbols) {
        var result = new ArrayList<String>();
        var head = (Pair) listOfSymbols;
        while (head != null) {
            var symbol = (Symbol) car(head);
            result.add(symbol.name());
            head = (Pair) cdr(head);
        }
        return result;
    }

    static Pair schemeList(Stream<Object> elements) {
        var elementList = elements.collect(Collectors.toList());
        Pair list = null;
        for (int i = elementList.size() - 1; i >= 0; i--) {
            var element = elementList.get(i);
            list = Pair.of(element, list);
        }
        return list;
    }

    static Pair schemeList(Object... objects) {
        return schemeList(Stream.of(objects));
    }

    static Pair symbolList(String... names) {
        return schemeList(Stream.of(names).map(Symbol::named));
    }
}
