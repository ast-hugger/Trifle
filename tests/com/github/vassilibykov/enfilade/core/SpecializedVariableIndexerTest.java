// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

package com.github.vassilibykov.enfilade.core;

import com.github.vassilibykov.enfilade.core.CompilerCodeGeneratorSpecialized.VariableIndexer;
import org.junit.Test;

import static com.github.vassilibykov.enfilade.core.JvmType.INT;
import static com.github.vassilibykov.enfilade.core.JvmType.REFERENCE;
import static org.junit.Assert.*;

public class SpecializedVariableIndexerTest {

    VariableIndexer indexer = new VariableIndexer(3);

    @Test
    public void testSameTypeAllocation() {
        assertEquals(3, indexer.allocate(REFERENCE));
        assertEquals(4, indexer.allocate(REFERENCE));
        assertEquals(5, indexer.allocate(REFERENCE));
        indexer.release(REFERENCE);
        indexer.release(REFERENCE);
        assertEquals(4, indexer.allocate(REFERENCE));
        assertEquals(6, indexer.frameSize());
    }

    @Test
    public void testInterleavedAllocation() {
        assertEquals(3, indexer.allocate(REFERENCE));
        assertEquals(4, indexer.allocate(INT));
        assertEquals(5, indexer.allocate(REFERENCE));
        assertEquals(6, indexer.allocate(INT));
        indexer.release(INT);
        indexer.release(REFERENCE);
        indexer.release(INT);
        assertEquals(5, indexer.allocate(REFERENCE));
        assertEquals(7, indexer.allocate(REFERENCE));
        indexer.release(REFERENCE);
        assertEquals(4, indexer.allocate(INT));
        assertEquals(6, indexer.allocate(INT));
        assertEquals(8, indexer.allocate(INT));
        assertEquals(9, indexer.allocate(INT));
        assertEquals(10, indexer.frameSize());
    }
}