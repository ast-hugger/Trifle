// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import org.junit.Test;

import java.util.List;

import static com.github.vassilibykov.trifle.scheme.Helpers.asListOfSymbolNames;
import static com.github.vassilibykov.trifle.scheme.Helpers.symbolList;
import static com.github.vassilibykov.trifle.scheme.TestHelpers.deeplyMatch;
import static com.github.vassilibykov.trifle.scheme.TestHelpers.listify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NormalizerTest {

    private Object normalize(Object input) {
        return new Normalizer(List.of(), List.of()).normalize(input);
    }

    @Test
    public void symbol() {
        Object result = normalize(Symbol.named("foo"));
        assertEquals("foo", ((Symbol) result).name());
    }

    @Test
    public void number() {
        assertEquals(42, normalize(42));
    }

    @Test
    public void string() {
        assertEquals("hello", normalize("hello"));
    }

    @Test
    public void normalizeNull() {
        assertEquals(null, normalize(null));
    }

    @Test
    public void begin() {
        var form = symbolList("begin", "foo", "bar");
        var result = asListOfSymbolNames(normalize(form));
        assertArrayEquals(new Object[]{"begin", "foo", "bar"}, result.toArray());
    }

    @Test
    public void callWithValues() {
        var call = listify("foo", "bar", "baz");
        assertTrue(deeplyMatch(call, normalize(call)));
    }

    @Test
    public void callWithCalls() {
        var form = listify("foo", listify("bar"), listify("baz"));
        var expected = listify(
            "let/a",
            listify("$t0", listify("bar")),
            listify(
                "let/a",
                listify("$t1", listify("baz")),
                listify("foo", "$t0", "$t1")
            )
        );
        assertTrue(deeplyMatch(expected, normalize(form)));
    }

    @Test
    public void ifWithValue() {
        var form = listify("if", "foo", "onTrue", "onFalse");
        var expected = listify("if/a", "foo", "onTrue", "onFalse");
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }

    @Test
    public void ifWithCall() {
        var form = listify("if", listify("foo"), "onTrue", "onFalse");
        var expected = listify(
            "let/a",
            listify("$t0", listify("foo")),
            listify("if/a", "$t0", "onTrue", "onFalse"));
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }

    @Test
    public void ifWithNoElse() {
        var form = listify("if", "test", "onTrue");
        var expected = listify("if/a", "test", "onTrue", null);
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }

    @Test
    public void letWithNoBindings() {
        var form = listify("let", listify(), "body");
        var expected = Symbol.named("body");
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }

    @Test
    public void letWithOneBinding() {
        var form = listify("let", listify(listify("foo", "bar")), "body");
        var expected = listify("let/a", listify("foo", "bar"), "body");
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }

    @Test
    public void letWithManyBinding() {
        var form = listify(
            "let",
            listify(
                listify("foo", "bar"),
                listify("zork", "quux")),
            "body");
        var expected = listify(
            "let/a",
            listify("foo", "bar"),
            listify(
                "let/a",
                listify("zork", "quux"),
                "body"));
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }

    @Test
    public void letWithManyExpressions() {
        var form = listify(
            "let",
            listify(listify("foo", "bar")),
            "one",
            "two");
        var expected = listify(
            "let/a",
            listify("foo", "bar"),
            listify("begin", "one", "two"));
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }

    @Test
    public void lambdaWithManyExpressions() {
        var form = listify("lambda", listify(), "one", "two", "three");
        var expected = listify("lambda/a", listify(), listify("begin", "one", "two", "three"));
        var result = normalize(form);
        assertTrue(deeplyMatch(expected, result));
    }
}