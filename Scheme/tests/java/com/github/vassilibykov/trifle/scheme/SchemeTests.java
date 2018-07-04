// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import org.junit.Before;
import org.junit.Test;

import java.io.CharArrayReader;

import static com.github.vassilibykov.trifle.scheme.Helpers.caaddr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadr;
import static com.github.vassilibykov.trifle.scheme.Helpers.car;
import static org.junit.Assert.*;

/**
 * Some not very systematic language-level tests.
 */
public class SchemeTests {

    private Scheme scheme;

    @Before
    public void setUp() throws Exception {
        scheme = new Scheme();
    }

    private Object run(String source) {
        return scheme.load(new CharArrayReader(source.toCharArray()));
    }

    @Test
    public void smokeTest() {
        assertEquals(42, run("42"));
    }

    @Test
    public void builtinCall() {
        assertEquals(7, run("(+ 3 4)"));
    }

    @Test
    public void functionDefinitionAndCall() {
        assertEquals(7, run("(define (add x y) (+ x y)) (add 3 4)"));
    }

    @Test
    public void quoteNumbers() {
        var list = run("(quote (1 2 (3)))");
        assertTrue(list instanceof Pair);
        assertEquals(1, car(list));
        assertEquals(2, cadr(list));
        assertEquals(3, caaddr(list));
    }

    @Test
    public void quoteOneSymbol() {
        var symbol = run("(quote hello)");
        assertTrue(symbol instanceof Symbol);
        assertEquals("hello", ((Symbol) symbol).name());
    }

    @Test
    public void macroexpander() {
        var result = run(
            "(define-macro (addm form) " +
                "(cons '+ (cons (cadr form) (cons (cadr form) null))))" +
            "(* (addm 3) (addm 4))"
        );
        assertEquals(48, result);
    }

    @Test
    public void global() {
        var result = run(
            "(define n 42)" +
            "(define (inc) (set! n (+ n 1)))" +
            "(inc)" +
            "(inc)" +
            "(inc)" +
            "n");
        assertEquals(45, result);
    }

    @Test
    public void testPairP() {
        assertEquals(true, run("(pair? (cons 1 2))"));
        assertEquals(false, run("(pair? 1)"));
    }
}