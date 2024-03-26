package com.pudonghot.tigon.mybatis.util;

import com.pudonghot.tigon.mybatis.*;
import lombok.val;
import java.util.*;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Modifier;
import com.pudonghot.tigon.mybatis.*;
import java.util.stream.Collectors;
import org.springframework.util.ReflectionUtils;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author Donghuang
 * @date Aug 31, 2020 13:21:19
 */
@Slf4j
public final class EntityUtils {

    public static final String ID = "id";

    /**
     * db col, wrap with quotationMark
     *
     * @param table table
     * @param col col
     * @return col
     */
    public static String quotationWrap(final String table, final String col) {
        val underscoreCol = StrUtils.camelToUnderscore(col);

        val concatenateCol = StrUtils.isNotBlank(table) &&
                !underscoreCol.contains(".") ?
                table + "." + underscoreCol : underscoreCol;

        val quotationMark = TigonMyBatisConfiguration.getStaticInstance()
                .getProperties().getQuotationMark();

        if (StrUtils.isBlank(quotationMark)) {
            return concatenateCol;
        }
        return Arrays.stream(concatenateCol.split("\\."))
                .map(c -> c.contains(quotationMark) ? c : quotationMark + c + quotationMark)
                .collect(Collectors.joining("."));
    }

    /**
     * db col, wrap with quotationMark
     *
     * @param col col
     * @return wrapped col
     */
    public static String quotationWrap(final String col) {
        return quotationWrap(null, col);
    }

    /**
     * primary key col
     *
     * @param clazz entity class
     * @return primary key col
     */
    public static String primaryKeyCol(final Class<?> clazz) {
        return quotationWrap(primaryKeyField(clazz));
    }

    /**
     * get primary key value
     * called in tigon-mybatis.xml
     *
     * @param entity entity object
     * @return primary key value
     */
    public static Object primaryKeyValue(final Object entity) {
        val entityClass = entity.getClass();

        if (AnnotationUtils.findAnnotation(entityClass, NoPrimaryKey.class) != null) {
            return "!!!NO_PRIMARY_KEY!!!";
        }

        val field = ReflectionUtils.findField(
                entityClass, primaryKeyField(entityClass));
        field.setAccessible(true);
        return ReflectionUtils.getField(field, entity);
    }

    /**
     * return entity object insert map
     * called in tigon-mybatis.xml
     *
     * @return insert map
     */
    public static Map<String, SqlParam> insertMap(final Object entity) {
        val entityClass = entity.getClass();

        val insertDefaultInsteadNull = TigonMyBatisConfiguration.getStaticInstance()
                .getProperties().isInsertDefaultInsteadNull();

        val mapRtn = new LinkedHashMap<String, SqlParam>();
        ReflectionUtils.doWithFields(entityClass, field -> {
            field.setAccessible(true);
            val fieldName = field.getName();

            val rawValue = getRawValue(true, entity, field);
            if (rawValue != null) {
                mapRtn.put(fieldName, rawValue);
                return;
            }

            val value = ReflectionUtils.getField(field, entity);
            if (insertDefaultInsteadNull && value == null) {
                mapRtn.put(fieldName, SqlParam.rawVal("default"));
                return;
            }

            mapRtn.put(fieldName, SqlParam.val(value));
        }, fieldFilter(entity, false));

        return mapRtn;
    }

    /**
     * return update map
     * called in tigon-mybatis.xml
     *
     * @return update map
     */
    public static Map<String, SqlParam> updateMap(final Object entity) {
        val entityClass = entity.getClass();
        val mapRtn = new LinkedHashMap<String, SqlParam>();
        ReflectionUtils.doWithFields(entityClass, field -> {
            field.setAccessible(true);
            val fieldName = field.getName();

            val rawValue = getRawValue(false, entity, field);
            if (rawValue != null) {
                mapRtn.put(fieldName, rawValue);
                return;
            }

            mapRtn.put(fieldName, SqlParam.val(ReflectionUtils.getField(field, entity)));

        }, fieldFilter(entity, true));

        return mapRtn;
    }

    static SqlParam getRawValue(final boolean forInsert, final Object entity, final Field field) {

        val annoRawValue =
                field.getAnnotation(RawValue.class);

        if (annoRawValue != null) {
            // not use for insert
            if (forInsert && !annoRawValue.forInsert()) {
                return null;
            }

            // not use for update
            if (!forInsert && !annoRawValue.forUpdate()) {
                return null;
            }

            val annoValue = annoRawValue.value();
            if (StrUtils.isNotBlank(annoValue)) {
                return SqlParam.rawVal(annoValue);
            }

            return SqlParam.rawVal(ReflectionUtils.getField(field, entity));
        }

        return null;
    }

    /**
     * return entity cols
     *
     * @param clazz entity class
     * @return entity cols
     */
    public static String cols(final Class<?> clazz) {
        val cols = new ArrayList<String>(16);
        ReflectionUtils.doWithFields(clazz,
            field -> cols.add(field.getName()),
            EntityUtils::isNormal);
        return cols.stream().map(EntityUtils::quotationWrap).collect(Collectors.joining(", "));
    }

    /**
     * get primary key field
     *
     * @param clazz entity class
     * @return primary key field name
     */
    static String primaryKeyField(final Class<?> clazz) {

        if (AnnotationUtils.findAnnotation(clazz, NoPrimaryKey.class) != null) {
            return "!!!NO_PRIMARY_KEY!!!";
        }

        val fields = new ArrayList<Field>(4);

        ReflectionUtils.doWithFields(clazz, fields::add,
                field -> isNormal(field) &&
                        (field.isAnnotationPresent(PrimaryKey.class) ||
                                ID.equalsIgnoreCase(field.getName())));

        AssertUtils.state(!fields.isEmpty(),
                () -> "No primary key found of entity class [" + clazz + "]");
        if (fields.size() == 1) {
            return fields.iterator().next().getName();
        }

        val annoPk = fields.stream()
                .filter(field -> field.isAnnotationPresent(PrimaryKey.class))
                .collect(Collectors.toList());

        AssertUtils.state(annoPk.size() < 2,
                () -> "Multiple @PrimaryKey found in entity class [" + clazz + "]");

        if (annoPk.size() == 1) {
            return annoPk.iterator().next().getName();
        }

        throw new IllegalStateException(
                "Could no decide primary key of entity class [" + clazz + "]");
    }

    static ReflectionUtils.FieldFilter fieldFilter(
            final Object entity,
            final boolean forUpdate) {

        return field -> {

            if (!isNormal(field)) {
                return false;
            }

            if (forUpdate) {

                // do not update id
                if (ID.equalsIgnoreCase(field.getName())) {
                    return false;
                }

                // do not update field marks @PrimaryKey
                if (field.isAnnotationPresent(PrimaryKey.class)) {
                    return false;
                }

                // do not update field marks @NotUpdate
                if (field.isAnnotationPresent(NotUpdate.class)) {
                    return false;
                }

                if (field.isAnnotationPresent(NotUpdateWhenNull.class)) {
                    field.setAccessible(true);
                    if (ReflectionUtils.getField(field, entity) == null) {
                        return false;
                    }
                }
            }

            return true;
        };
    }

    static boolean isNormal(final Field field) {
        val modifiers = field.getModifiers();
        return !Modifier.isTransient(modifiers) &&
                !Modifier.isPublic(modifiers) &&
                !Modifier.isStatic(modifiers) &&
                !field.isAnnotationPresent(Transient.class);
    }
}
