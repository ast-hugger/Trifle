// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.github.vassilibykov.enfilade.core.TypeCategory.BOOL;
import static com.github.vassilibykov.enfilade.core.TypeCategory.INT;
import static com.github.vassilibykov.enfilade.core.TypeCategory.REFERENCE;
import static com.github.vassilibykov.enfilade.core.TypeCategory.VOID;
import static org.junit.Assert.*;

public class TypeCategoryTest {

    private static class Expectation {
        private final TypeCategory first;
        private final TypeCategory second;
        private final TypeCategory result;

        Expectation(TypeCategory first, TypeCategory second, TypeCategory result) {
            this.first = first;
            this.second = second;
            this.result = result;
        }
    }

    private static Expectation expect(TypeCategory first, TypeCategory second, TypeCategory result) {
        return new Expectation(first, second, result);
    }

    private List<Expectation> unionExpectations = Arrays.asList(
        expect(INT, INT, INT),
        expect(INT, BOOL, REFERENCE),
        expect(INT, REFERENCE, REFERENCE),
        expect(INT, VOID, INT),

        expect(BOOL, BOOL, BOOL),
        expect(BOOL, INT, REFERENCE),
        expect(BOOL, REFERENCE, REFERENCE),
        expect(BOOL, VOID, BOOL),

        expect(REFERENCE, REFERENCE, REFERENCE),
        expect(REFERENCE, INT, REFERENCE),
        expect(REFERENCE, BOOL, REFERENCE),
        expect(REFERENCE, VOID, REFERENCE),

        expect(VOID, VOID, VOID),
        expect(VOID, INT, INT),
        expect(VOID, BOOL, BOOL),
        expect(VOID, REFERENCE, REFERENCE)
    );

    @Test
    public void testUnify() {
        unionExpectations.forEach(each -> assertEquals(each.result, each.first.union(each.second)));

    }
}