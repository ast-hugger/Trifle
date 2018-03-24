// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ExpressionTypeTest {

    private static final ExpressionType UNKNOWN = ExpressionType.unknown();
    private static final ExpressionType KNOWN_VOID = ExpressionType.known(TypeCategory.VOID);
    private static final ExpressionType KNOWN_BOOL = ExpressionType.known(TypeCategory.BOOL);
    private static final ExpressionType KNOWN_INT = ExpressionType.known(TypeCategory.INT);
    private static final ExpressionType KNOWN_REFERENCE = ExpressionType.known(TypeCategory.REFERENCE);

    private static class Expectation {
        private final ExpressionType first;
        private final ExpressionType second;
        private final ExpressionType result;

        Expectation(ExpressionType first, ExpressionType second, ExpressionType result) {
            this.first = first;
            this.second = second;
            this.result = result;
        }
    }

    private static Expectation expect(ExpressionType first, ExpressionType second, ExpressionType result) {
        return new Expectation(first, second, result);
    }

    private static List<Expectation> UNION_EXPECTATIONS = Arrays.asList(
        expect(UNKNOWN, UNKNOWN, UNKNOWN),
        expect(UNKNOWN, KNOWN_VOID, UNKNOWN),
        expect(UNKNOWN, KNOWN_BOOL, UNKNOWN),
        expect(UNKNOWN, KNOWN_INT, UNKNOWN),
        expect(UNKNOWN, KNOWN_REFERENCE, UNKNOWN),

        expect(KNOWN_VOID, UNKNOWN, UNKNOWN),
        expect(KNOWN_VOID, KNOWN_VOID, KNOWN_VOID),
        expect(KNOWN_VOID, KNOWN_BOOL, KNOWN_BOOL),
        expect(KNOWN_VOID, KNOWN_INT, KNOWN_INT),
        expect(KNOWN_VOID, KNOWN_REFERENCE, KNOWN_REFERENCE),

        expect(KNOWN_BOOL, UNKNOWN, UNKNOWN),
        expect(KNOWN_BOOL, KNOWN_VOID, KNOWN_BOOL),
        expect(KNOWN_BOOL, KNOWN_BOOL, KNOWN_BOOL),
        expect(KNOWN_BOOL, KNOWN_INT, KNOWN_REFERENCE),
        expect(KNOWN_BOOL, KNOWN_REFERENCE, KNOWN_REFERENCE),

        expect(KNOWN_INT, UNKNOWN, UNKNOWN),
        expect(KNOWN_INT, KNOWN_VOID, KNOWN_INT),
        expect(KNOWN_INT, KNOWN_BOOL, KNOWN_REFERENCE),
        expect(KNOWN_INT, KNOWN_INT, KNOWN_INT),
        expect(KNOWN_INT, KNOWN_REFERENCE, KNOWN_REFERENCE),

        expect(KNOWN_REFERENCE, UNKNOWN, UNKNOWN),
        expect(KNOWN_REFERENCE, KNOWN_VOID, KNOWN_REFERENCE),
        expect(KNOWN_REFERENCE, KNOWN_BOOL, KNOWN_REFERENCE),
        expect(KNOWN_REFERENCE, KNOWN_INT, KNOWN_REFERENCE),
        expect(KNOWN_REFERENCE, KNOWN_REFERENCE, KNOWN_REFERENCE)
    );

    @Test
    public void testUnion() {
        UNION_EXPECTATIONS.forEach(
            each -> assertEquals(each.result, each.first.union(each.second)));
    }

    private static List<Expectation> OPPORTUNISTIC_UNION_EXPECTATIONS = Arrays.asList(
        expect(UNKNOWN, UNKNOWN, UNKNOWN),
        expect(UNKNOWN, KNOWN_VOID, KNOWN_VOID),
        expect(UNKNOWN, KNOWN_BOOL, KNOWN_BOOL),
        expect(UNKNOWN, KNOWN_INT, KNOWN_INT),
        expect(UNKNOWN, KNOWN_REFERENCE, KNOWN_REFERENCE),

        expect(KNOWN_VOID, UNKNOWN, KNOWN_VOID),
        expect(KNOWN_VOID, KNOWN_VOID, KNOWN_VOID),
        expect(KNOWN_VOID, KNOWN_BOOL, KNOWN_BOOL),
        expect(KNOWN_VOID, KNOWN_INT, KNOWN_INT),
        expect(KNOWN_VOID, KNOWN_REFERENCE, KNOWN_REFERENCE),

        expect(KNOWN_BOOL, UNKNOWN, KNOWN_BOOL),
        expect(KNOWN_BOOL, KNOWN_VOID, KNOWN_BOOL),
        expect(KNOWN_BOOL, KNOWN_BOOL, KNOWN_BOOL),
        expect(KNOWN_BOOL, KNOWN_INT, KNOWN_REFERENCE),
        expect(KNOWN_BOOL, KNOWN_REFERENCE, KNOWN_REFERENCE),

        expect(KNOWN_INT, UNKNOWN, KNOWN_INT),
        expect(KNOWN_INT, KNOWN_VOID, KNOWN_INT),
        expect(KNOWN_INT, KNOWN_BOOL, KNOWN_REFERENCE),
        expect(KNOWN_INT, KNOWN_INT, KNOWN_INT),
        expect(KNOWN_INT, KNOWN_REFERENCE, KNOWN_REFERENCE),

        expect(KNOWN_REFERENCE, UNKNOWN, KNOWN_REFERENCE),
        expect(KNOWN_REFERENCE, KNOWN_VOID, KNOWN_REFERENCE),
        expect(KNOWN_REFERENCE, KNOWN_BOOL, KNOWN_REFERENCE),
        expect(KNOWN_REFERENCE, KNOWN_INT, KNOWN_REFERENCE),
        expect(KNOWN_REFERENCE, KNOWN_REFERENCE, KNOWN_REFERENCE)
    );

    @Test
    public void testOpportunisticUnion() {
        OPPORTUNISTIC_UNION_EXPECTATIONS.forEach(
            each -> assertEquals(each.result, each.first.opportunisticUnion(each.second)));
    }

}