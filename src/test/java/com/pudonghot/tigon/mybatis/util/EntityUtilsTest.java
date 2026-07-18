package com.pudonghot.tigon.mybatis.util;

import java.util.Map;
import com.pudonghot.tigon.mybatis.NoPrimaryKey;
import com.pudonghot.tigon.mybatis.NotUpdate;
import com.pudonghot.tigon.mybatis.NotUpdateWhenNull;
import com.pudonghot.tigon.mybatis.PrimaryKey;
import com.pudonghot.tigon.mybatis.RawValue;
import com.pudonghot.tigon.mybatis.SqlParam;
import com.pudonghot.tigon.mybatis.TigonMyBatisConfiguration;
import com.pudonghot.tigon.mybatis.TigonMyBatisProperties;
import com.pudonghot.tigon.mybatis.Transient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityUtilsTest {

    private TigonMyBatisProperties properties;

    @BeforeEach
    void configure() {
        properties = new TigonMyBatisProperties();
        properties.setQuotationMark("`");
        properties.setInsertDefaultInsteadNull(false);

        var configuration = new TigonMyBatisConfiguration();
        ReflectionTestUtils.setField(configuration, "properties", properties);
        ReflectionTestUtils.setField(
            TigonMyBatisConfiguration.class, "STATIC_INSTANCE", configuration);
    }

    @Test
    void wrapsSimpleColumnsAndPreservesExpressions() {
        assertEquals("`display_name`", EntityUtils.quotationWrap("displayName"));
        assertEquals("`users`.`display_name`", EntityUtils.quotationWrap("users", "displayName"));
        assertEquals("users.display_name", EntityUtils.quotationWrap("users.displayName"));
        assertEquals("count(id)", EntityUtils.quotationWrap("count(id)"));
    }

    @Test
    void discoversColumnsAndExplicitPrimaryKey() {
        var entity = sampleEntity();

        assertEquals("key", EntityUtils.primaryKeyField(SampleEntity.class));
        assertEquals("`key`", EntityUtils.primaryKeyCol(SampleEntity.class));
        assertEquals(42L, EntityUtils.primaryKeyValue(entity));
        assertEquals(
            "`key`, `display_name`, `immutable`, `optional`, `token`, `updated_at`",
            EntityUtils.cols(SampleEntity.class));
    }

    @Test
    void createsInsertParametersFromFieldsAndRawValueAnnotations() {
        Map<String, SqlParam> insert = EntityUtils.insertMap(sampleEntity());

        assertEquals(
            java.util.List.of("key", "displayName", "immutable", "optional", "token", "updatedAt"),
            java.util.List.copyOf(insert.keySet()));
        assertEquals(42L, insert.get("key").getValue());
        assertEquals("Alice", insert.get("displayName").getValue());
        assertNull(insert.get("optional").getValue());
        assertFalse(insert.get("optional").isRaw());
        assertEquals("hex(randomblob(8))", insert.get("token").getValue());
        assertTrue(insert.get("token").isRaw());
        assertNull(insert.get("updatedAt").getValue());
        assertFalse(insert.get("updatedAt").isRaw());
    }

    @Test
    void replacesNullInsertValuesWithDatabaseDefaultWhenConfigured() {
        properties.setInsertDefaultInsteadNull(true);

        Map<String, SqlParam> insert = EntityUtils.insertMap(sampleEntity());

        assertEquals("default", insert.get("optional").getValue());
        assertTrue(insert.get("optional").isRaw());
        assertEquals("default", insert.get("updatedAt").getValue());
        assertTrue(insert.get("updatedAt").isRaw());
    }

    @Test
    void filtersProtectedFieldsAndUsesRawValuesForUpdates() {
        Map<String, SqlParam> update = EntityUtils.updateMap(sampleEntity());

        assertFalse(update.containsKey("key"));
        assertFalse(update.containsKey("immutable"));
        assertFalse(update.containsKey("optional"));
        assertEquals("Alice", update.get("displayName").getValue());
        assertNull(update.get("token").getValue());
        assertFalse(update.get("token").isRaw());
        assertEquals("CURRENT_TIMESTAMP", update.get("updatedAt").getValue());
        assertTrue(update.get("updatedAt").isRaw());
    }

    @Test
    void marksNullableBatchFieldsToKeepTheirExistingValues() {
        Map<String, SqlParam> update = EntityUtils.batchUpdateMap(sampleEntity());

        assertEquals(42L, update.get("key").getValue());
        assertFalse(update.containsKey("immutable"));
        assertTrue(update.get("optional").isIgnoreNull());
        assertFalse(update.get("displayName").isIgnoreNull());
    }

    @Test
    void handlesEntitiesWithoutOrWithAmbiguousPrimaryKeys() {
        assertEquals("!!!NO_PRIMARY_KEY!!!", EntityUtils.primaryKeyField(AuditView.class));
        assertEquals("!!!NO_PRIMARY_KEY!!!", EntityUtils.primaryKeyValue(new AuditView()));
        assertEquals("businessKey", EntityUtils.primaryKeyField(EntityWithIdAndKey.class));
        assertThrows(IllegalStateException.class,
            () -> EntityUtils.primaryKeyField(EntityWithTwoKeys.class));
        assertThrows(IllegalStateException.class,
            () -> EntityUtils.primaryKeyField(EntityWithoutKey.class));
    }

    private static SampleEntity sampleEntity() {
        var entity = new SampleEntity();
        entity.key = 42L;
        entity.displayName = "Alice";
        entity.immutable = "fixed";
        entity.optional = null;
        entity.calculated = "ignored";
        entity.javaTransient = "ignored";
        entity.publicValue = "ignored";
        return entity;
    }

    static class SampleEntity {
        @PrimaryKey
        Long key;
        String displayName;
        @NotUpdate
        String immutable;
        @NotUpdateWhenNull
        String optional;
        @RawValue(value = "hex(randomblob(8))", forUpdate = false)
        String token;
        @RawValue(value = "CURRENT_TIMESTAMP", forInsert = false, forUpdate = true)
        String updatedAt;
        @Transient
        String calculated;
        transient String javaTransient;
        public String publicValue;
        static String staticValue;
    }

    @NoPrimaryKey
    static class AuditView {
        String message;
    }

    static class EntityWithIdAndKey {
        Long id;
        @PrimaryKey
        String businessKey;
    }

    static class EntityWithTwoKeys {
        @PrimaryKey
        Long first;
        @PrimaryKey
        Long second;
    }

    static class EntityWithoutKey {
        String name;
    }
}
