package com.pudonghot.tigon.mybatis;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchTest {

    @BeforeAll
    static void configureQuotationMark() {
        var properties = new TigonMyBatisProperties();
        properties.setQuotationMark("`");

        var configuration = new TigonMyBatisConfiguration();
        ReflectionTestUtils.setField(configuration, "properties", properties);
        ReflectionTestUtils.setField(
            TigonMyBatisConfiguration.class, "STATIC_INSTANCE", configuration);
    }

    @Test
    void assemblesComparisonAndNullCriteriaInInsertionOrder() {
        var search = Search.of(7)
            .eq("displayName", "Alice")
            .ne("deletedAt", null)
            .between("score", 10, 20)
            .notBetween("age", 30, 40)
            .gte("level", 2)
            .lt("level", 5);

        assertEquals(
            "`id` = 7 and `display_name` = Alice and `deleted_at` is not null"
                + " and `score` between 10 and 20 and `age` not between 30 and 40"
                + " and `level` >= 2 and `level` < 5",
            render(search));
    }

    @Test
    void convertsCollectionsAndPrimitiveArraysToSetCriteria() {
        var search = Search.of()
            .eq("id", new int[] {1, 2})
            .ne("status", List.of("DELETED", "DISABLED"));

        assertEquals(
            "`id` in (1, 2) and `status` not in (DELETED, DISABLED)",
            render(search));
    }

    @Test
    void assemblesNestedBooleanGroupsWithParentheses() {
        var roles = Search.of()
            .or("role", "ADMIN")
            .or("role", "OWNER");
        var search = Search.of()
            .table("users")
            .eq("active", true)
            .and(roles)
            .or(Search.of("locked", false));

        assertEquals(
            "`users`.`active` = true and (`users`.`role` = ADMIN or `users`.`role` = OWNER)"
                + " or `users`.`locked` = false",
            render(search));
    }

    @Test
    void supportsLikeHelpersAndCustomCriteria() {
        var search = Search.of()
            .startsWith("name", "Al")
            .endsWith("email", "example.com")
            .notContains("remark", "blocked")
            .build(arg -> arg.addSql("score % 2 = ").addParam(1));

        assertEquals(
            "`name` like Al% and `email` like %example.com"
                + " and `remark` not like %blocked% and score % 2 = 1",
            render(search));
    }

    @Test
    void keepsSqlFragmentsSeparateFromBoundParameterValues() {
        var assembled = Search.of("name", "Alice").assemble();

        assertEquals(2, assembled.size());
        assertTrue(assembled.get(0) instanceof SqlParam);
        assertTrue(((SqlParam) assembled.get(0)).isRaw());
        assertEquals("`name` = ", ((SqlParam) assembled.get(0)).getValue());
        assertEquals("Alice", assembled.get(1));
        assertFalse(assembled.get(1) instanceof SqlParam);
    }

    @Test
    void clonesPagingOrderingDistinctAndAttributesIndependently() {
        var original = Search.of("status", "ACTIVE")
            .table("users")
            .desc("createdAt")
            .offset(20)
            .limit(10)
            .distinct(true)
            .attr("forUpdate", true);

        var copy = Search.clone(original);
        copy.clearCriteria().clearOrders().offset(0).limit(5).attr("forUpdate", false);

        assertTrue(original.hasCriterion("status"));
        assertTrue(original.hasOrder());
        assertEquals(20, original.offset());
        assertEquals(10, original.limit());
        assertTrue(original.hasDistinct());
        assertTrue(original.trueAttr("forUpdate"));
        assertEquals("DESC", original.orders().get("`users`.`created_at`"));

        assertFalse(copy.hasCriterion());
        assertFalse(copy.hasOrder());
        assertEquals(0, copy.offset());
        assertEquals(5, copy.limit());
        assertFalse(copy.trueAttr("forUpdate"));
    }

    private static String render(final Search search) {
        return search.assemble().stream()
            .map(String::valueOf)
            .collect(Collectors.joining());
    }
}
