package com.pudonghot.tigon.mybatis;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseQueryMapperTest {

    @Test
    void batchScanRequestsEveryPageAndUsesRemainingSizeForLastPage() {
        var mapper = mapperCallingDefaultMethods();
        var search = Search.of();
        var requestedPages = new ArrayList<String>();
        var scannedBatches = new ArrayList<List<Integer>>();

        boolean found = mapper.batchScan(
            3,
            search,
            ignored -> 7,
            page -> {
                requestedPages.add(page.offset() + ":" + page.limit());
                return java.util.stream.IntStream
                    .range(page.offset(), page.offset() + page.limit())
                    .boxed()
                    .toList();
            },
            scannedBatches::add);

        assertTrue(found);
        assertEquals(List.of("0:3", "3:3", "6:1"), requestedPages);
        assertEquals(
            List.of(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6)),
            scannedBatches);
        assertEquals(6, search.offset());
        assertEquals(1, search.limit());
    }

    @Test
    void batchScanDoesNotQueryOrInvokeScannerWhenThereAreNoRows() {
        var mapper = mapperCallingDefaultMethods();
        var listCalls = new int[1];
        var scannerCalls = new int[1];

        boolean found = mapper.batchScan(
            10,
            Search.of(),
            ignored -> 0,
            ignored -> {
                listCalls[0]++;
                return List.of();
            },
            ignored -> scannerCalls[0]++);

        assertFalse(found);
        assertEquals(0, listCalls[0]);
        assertEquals(0, scannerCalls[0]);
    }

    private static BaseQueryMapper<Integer, Integer> mapperCallingDefaultMethods() {
        return new TestQueryMapper();
    }

    private static class TestQueryMapper implements BaseQueryMapper<Integer, Integer> {
        @Override
        public int count(final Search search) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean exists(final Search search) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer find(final Search search) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer find(final Integer primaryKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T findCol(final String col, final Search search) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> list(final Search search) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> List<T> listCol(final String col, final Search search) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> select(final String selectExpr, final Search search) {
            throw new UnsupportedOperationException();
        }
    }
}
