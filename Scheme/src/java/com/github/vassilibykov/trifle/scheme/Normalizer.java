// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.trifle.scheme;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import static com.github.vassilibykov.trifle.scheme.Constants.Symbols.BEGIN;
import static com.github.vassilibykov.trifle.scheme.Constants.Symbols.IF_A;
import static com.github.vassilibykov.trifle.scheme.Constants.Symbols.LAMBDA_A;
import static com.github.vassilibykov.trifle.scheme.Constants.Symbols.LET_A;
import static com.github.vassilibykov.trifle.scheme.Constants.Symbols.SET_BANG;
import static com.github.vassilibykov.trifle.scheme.Helpers.caar;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadar;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadddr;
import static com.github.vassilibykov.trifle.scheme.Helpers.caddr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cadr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cdddr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cddr;
import static com.github.vassilibykov.trifle.scheme.Helpers.cdr;
import static com.github.vassilibykov.trifle.scheme.Helpers.schemeList;

/**
 * Rewrites a Scheme expression to convert it into A-normal form.
 *
 * <p>The essence of normalization is captured by just three methods:
 * {@link #normalizeCall(Pair)}, {@link #normalizeIf(Pair)}, and
 * {@link #isValue(Object)}. This code from the original
 * A-normalization paper might be easier to follow than Java:
 *
 * <pre>{@code
 * (define (normalize-term M) (normalize M (λ (x) x)))
 *
 * (define (normalize M k)
 *   (match M
 *     [`(λ ,params ,body)
 *       (k `(λ ,params ,(normalize-term body)))]
 *
 *     [`(let ([,x ,M1]) ,M2)
 *       (normalize M1 (λ (N1)
 *        `(let ([,x ,N1])
 *          ,(normalize M2 k))))]
 *
 *     [`(if ,M1 ,M2 ,M3)
 *       (normalize-name M1 (λ (t)
 *        (k `(if ,t ,(normalize-term M2)
 *                   ,(normalize-term M3)))))]
 *
 *     [`(,Fn . ,M*)
 *       (normalize-name Fn (λ (t)
 *        (normalize-name* M* (λ (t*)
 *         (k `(,t . ,t*))))))]
 *
 *     [(? Value?)             (k M)]))
 *
 * (define (normalize-name M k)
 *   (normalize M (λ (N)
 *     (if (Value? N) (k N)
 *         (let ([t (gensym)])
 *          `(let ([,t ,N]) ,(k t)))))))
 *
 * (define (normalize-name* M* k)
 *   (if (null? M*)
 *       (k '())
 *       (normalize-name (car M*) (λ (t)
 *        (normalize-name* (cdr M*) (λ (t*)
 *         (k `(,t . ,t*))))))))
 * }</pre>
 */
class Normalizer {

    private final Collection<String> primitiveNames;
    private final Collection<String> globalNames;

    Normalizer(Collection<String> primitiveNames, Collection<String> globalNames) {
        this.primitiveNames = primitiveNames;
        this.globalNames = globalNames;
    }

    Object normalize(Object term) {
        if (isValue(term)) return term;
        return term instanceof Pair ? normalizeForm((Pair) term) : term;
    }

    private Object normalizeForm(Pair form) {
        var head = form.car();
        if (head instanceof Symbol) {
            var name = ((Symbol) head).name();
            switch (name) {
                case Constants.BEGIN:
                    return normalizeBegin(form);
                case Constants.IF:
                    return normalizeIf(form);
                case Constants.LAMBDA:
                    return normalizeLambda(form);
                case Constants.LET:
                    return normalizeLet(form);
                case Constants.SET_BANG:
                    return normalizeSet(form);
                default:
                    return normalizeCall(form);
            }
        } else {
            return normalizeCall(form);
        }
    }

    private Object normalizeBegin(Pair form) {
        return Pair.of(
            BEGIN,
            normalizeList((Pair) form.cdr()));
    }

    /**
     * The standard call may have arbitrary expressions as arguments.
     * The normalized call must have atomic expressions as arguments.
     */
    private Object normalizeCall(Pair form) {
        var head = form.car();
        var args = (Pair) form.cdr();
        if (isValue(head)) {
            return normalizeArgList(args, normalizedArgs -> Pair.of(head, normalizedArgs));
        } else {
            var temp = gensym();
            var init = normalize(head);
            return schemeList(
                LET_A,
                schemeList(temp, init),
                normalizeArgList(args, normalizedArgs -> Pair.of(temp, normalizedArgs)));
        }
    }

    private Object normalizeArgList(Pair list, Function<Pair, Object> restRewriter) {
        if (list == null) return restRewriter.apply(null);
        var head = list.car();
        var rest = (Pair) list.cdr();
        if (isValue(head)) {
            return normalizeArgList(rest, rewrittenArgs -> restRewriter.apply(Pair.of(head, rewrittenArgs)));
        } else {
            var temp = gensym();
            return schemeList(
                LET_A,
                schemeList(temp, normalize(head)),
                normalizeArgList(rest, rewrittenArgs -> restRewriter.apply(Pair.of(temp, rewrittenArgs))));
        }
    }

    /**
     * The standard {@code if} may have 1 or 2 clauses and a non-atomic test
     * expression. The normalized {@code if/a} should have an atomic test
     * expression and 2 clauses.
     */
    private Object normalizeIf(Pair form) {
        var condition = cadr(form);
        var thenClause = caddr(form);
        var normalizedThen = normalize(thenClause);
        Object elseClause = null;
        if (cdddr(form) != null) {
            elseClause = cadddr(form);
        }
        var normalizedElse = normalize(elseClause);
        if (isValue(condition)) {
            return schemeList(IF_A, condition, normalizedThen, normalizedElse);
        } else {
            var temp = gensym();
            return schemeList(
                LET_A,
                schemeList(temp, normalize(condition)),
                schemeList(IF_A, temp, normalizedThen, normalizedElse));
        }
    }

    /**
     * The standard {@code lambda} has the body of one or more expressions. The
     * normalized {@code lambda/a} must have a single body expression.
     */
    private Object normalizeLambda(Pair form) {
        var params = cadr(form);
        Object body;
        if (cdddr(form) == null) { // single expression body
            body = caddr(form);
        } else {
            body = Pair.of(BEGIN, cddr(form));
        }
        return schemeList(
            LAMBDA_A,
            params,
            normalize(body));
    }

    /**
     * The standard {@code let} form has a sequence of zero or more variable
     * bindings (i.e. a list of two-element lists, possibly empty, as the second
     * element), and one or more body expression. The normalized {@code let/a}
     * form has a single binding (a two-element list as the second element) and
     * a single body expression.
     */
    private Object normalizeLet(Pair form) {
        var bindings = (Pair) cadr(form);
        Object body;
        if (cdddr(form) == null) { // single expression body
            body = caddr(form);
        } else {
            body = Pair.of(BEGIN, cddr(form));
        }
        return rewriteLetBindings(bindings, normalize(body));
    }

    private Object rewriteLetBindings(Pair bindings, Object body) {
        if (bindings == null) return body;
        var variable = caar(bindings);
        var initializer = normalize(cadar(bindings));
        return schemeList(
            LET_A,
            schemeList(variable, initializer),
            rewriteLetBindings((Pair) cdr(bindings), body));
    }

    /**
     * The standard set allows an arbitrary value expression.
     * The normalized set requires an atomic value expression.
     */
    private Object normalizeSet(Pair form) {
        var variable = cadr(form);
        var valueExpr = caddr(form);
        if (isValue(valueExpr)) {
            return form;
        } else {
            var temp = gensym();
            return schemeList(
                LET_A,
                schemeList(temp, normalize(valueExpr)),
                schemeList(SET_BANG, variable, temp));
        }
    }

    private Object normalizeList(Pair list) {
        if (list == null) return null;
        var head = list.car();
        return Pair.of(normalize(head), normalizeList((Pair) list.cdr()));
    }

    private boolean isValue(Object term) {
        if (!(term instanceof Pair)) {
            return !(term instanceof Symbol && globalNames.contains(((Symbol) term).name()));
        }
        var head = ((Pair) term).car();
        if (head instanceof Symbol && primitiveNames.contains(((Symbol) head).name())) return true;
        return Objects.equals(head, Constants.Symbols.QUOTE);
    }

    private int serial = 0;

    private Symbol gensym() {
        return Symbol.named("$t" + serial++);
    }
}
