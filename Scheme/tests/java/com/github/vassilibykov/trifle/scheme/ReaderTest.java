// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.vassilibykov.trifle.scheme.Helpers.caadr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadadr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadr;
import static com.github.vassilibykov.trifle.scheme.Helpers.car;
import static com.github.vassilibykov.trifle.scheme.Helpers.cddr;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("SimplifiableJUnitAssertion")
public class ReaderTest {

    @Test
    public void symbol() {
        var symbol = (Symbol) Reader.read("asdf");
        assertEquals("asdf", symbol.name());
    }

    @Test
    public void symbolWithDigits() {
        var symbol = (Symbol) Reader.read("asdf1234");
        assertEquals("asdf1234", symbol.name());
    }

    @Test
    public void symbolWithFunnies() {
        var symbol = (Symbol) Reader.read("a/s-d");
        assertEquals("a/s-d", symbol.name());
    }

    @Test
    public void string() {
        assertEquals("hello", Reader.read("\"hello\""));
    }

    @Test
    public void stringInList() {
        var list = asJavaList(Reader.read("(\"hello\")"));
        assertEquals("hello", list.get(0));
    }

    @Test
    public void newlinesShouldBeSkipped() {
        var symbol = (Symbol) Reader.read("\n\nasdf");
        assertEquals("asdf", symbol.name());
    }

    @Test
    public void number() {
        var number = (Integer) Reader.read("1234");
        assertEquals(1234, (int) number);
    }

    @Test
    public void list() {
        var list = asJavaList(Reader.read("(foo bar)"));
        assertArrayEquals(new String[]{"foo", "bar"}, symbolNames(list));
    }

    @Test
    public void nestedLists() {
        var list = asJavaList(Reader.read("((foo) (bar))"));
        var list1 = asJavaList(list.get(0));
        var list2 = asJavaList(list.get(1));
        assertArrayEquals(new String[]{"foo"}, symbolNames(list1));
        assertArrayEquals(new String[]{"bar"}, symbolNames(list2));
    }

    @Test
    public void dottedPair() {
        var pair = Reader.read("(foo . bar)");
        assertTrue(pair instanceof Pair);
        var cell = (Pair) pair;
        assertEquals("foo", ((Symbol) cell.car()).name());
        assertEquals("bar", ((Symbol) cell.cdr()).name());
    }

    @Test
    public void dottedList() {
        var pair = Reader.read("(foo bar . zork)");
        assertTrue(pair instanceof Pair);
        var cell = (Pair) pair;
        assertEquals("foo", ((Symbol) cell.car()).name());
        cell = (Pair) cell.cdr();
        assertEquals("bar", ((Symbol) cell.car()).name());
        assertEquals("zork", ((Symbol) cell.cdr()).name());
    }

    @Test
    public void quotedSymbol() {
        var pair = Reader.read("'hello");
        assertTrue(pair instanceof Pair);
        assertEquals("quote", ((Symbol) car(pair)).name());
        assertEquals("hello", ((Symbol) cadr(pair)).name());
        assertEquals(null, cddr(pair));
    }

    @Test
    public void quotedList() {
        var pair = Reader.read("'(hello bye)");
        assertTrue(pair instanceof Pair);
        assertEquals("quote", ((Symbol) car(pair)).name());
        assertEquals("hello", ((Symbol) caadr(pair)).name());
        assertEquals("bye", ((Symbol) cadadr(pair)).name());
        assertEquals(null, cddr(pair));
    }

    private List<Object> asJavaList(Object anObject) {
        var list = (Pair) anObject;
        var result = new ArrayList<Object>();
        while (list != null) {
            result.add(list.car());
            list = (Pair) list.cdr();
        }
        return result;
    }

    private String[] symbolNames(List<Object> contents) {
        return contents.stream().map(each -> ((Symbol) each).name()).toArray(String[]::new);
    }
}