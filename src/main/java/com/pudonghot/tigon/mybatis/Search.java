package com.pudonghot.tigon.mybatis;

import lombok.Getter;
import lombok.val;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.lang.reflect.Array;
import lombok.EqualsAndHashCode;
import java.util.function.Consumer;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import com.pudonghot.tigon.mybatis.util.StrUtils;
import com.pudonghot.tigon.mybatis.util.EntityUtils;
import static com.pudonghot.tigon.mybatis.Criterion.Type.EQ;
import static com.pudonghot.tigon.mybatis.Criterion.Type.GT;
import static com.pudonghot.tigon.mybatis.Criterion.Type.AND;
import static com.pudonghot.tigon.mybatis.Criterion.Type.GTE;
import static com.pudonghot.tigon.mybatis.Criterion.Type.IN;
import static com.pudonghot.tigon.mybatis.Criterion.Type.LT;
import static com.pudonghot.tigon.mybatis.Criterion.Type.NE;
import static com.pudonghot.tigon.mybatis.Criterion.Type.OR;
import static com.pudonghot.tigon.mybatis.Criterion.Type.LTE;
import static com.pudonghot.tigon.mybatis.Criterion.Type.LIKE;
import static com.pudonghot.tigon.mybatis.Criterion.Type.BETWEEN;
import static com.pudonghot.tigon.mybatis.Criterion.Type.BUILDER;
import static com.pudonghot.tigon.mybatis.Criterion.Type.NOT_IN;
import static com.pudonghot.tigon.mybatis.Criterion.Type.IS_NULL;
import static com.pudonghot.tigon.mybatis.Criterion.Type.NOT_LIKE;
import static com.pudonghot.tigon.mybatis.Criterion.Type.IS_NOT_NULL;
import static com.pudonghot.tigon.mybatis.Criterion.Type.NOT_BETWEEN;
import static com.pudonghot.tigon.mybatis.util.FnGetterUtils.getFieldName;

/**
 * @author Donghuang
 * @date May 12, 2015 3:00:40 PM
 */
@EqualsAndHashCode
public class Search implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * order
     */
    public enum Order {
        ASC,
        DESC
    }

    private static final Map<Criterion.Type, Consumer<ProcArg>> PROCESSORS;

    static {
        PROCESSORS = new HashMap<>(16);

        PROCESSORS.put(EQ, arg -> arg.addSql(arg.getCol() + " = ").addParam());
        PROCESSORS.put(NE, arg -> arg.addSql(arg.getCol() + " <> ").addParam());
        PROCESSORS.put(LIKE, arg -> arg.addSql(arg.getCol() + " like ").addParam());
        PROCESSORS.put(NOT_LIKE, arg -> arg.addSql(arg.getCol() + " not like ").addParam());
        PROCESSORS.put(LT, arg -> arg.addSql(arg.getCol() + " < ").addParam());
        PROCESSORS.put(LTE, arg -> arg.addSql(arg.getCol() + " <= ").addParam());
        PROCESSORS.put(GT, arg -> arg.addSql(arg.getCol() + " > ").addParam());
        PROCESSORS.put(GTE, arg -> arg.addSql(arg.getCol() + " >= ").addParam());
        PROCESSORS.put(IS_NULL, arg -> arg.addSql(arg.getCol() + " is null"));
        PROCESSORS.put(IS_NOT_NULL, arg -> arg.addSql(arg.getCol() + " is not null"));
        PROCESSORS.put(BETWEEN, arg -> {
            val valIt = arg.getCriterion()
                .getValues().iterator();
            arg.addSql(arg.getCol() + " between ")
                .addParam(valIt.next())
                .addSql(" and ")
                .addParam(valIt.next());
        });
        PROCESSORS.put(NOT_BETWEEN, arg -> {
            val valIt = arg.getCriterion()
                .getValues().iterator();
            arg.addSql(arg.getCol() + " not between ")
                .addParam(valIt.next())
                .addSql(" and ")
                .addParam(valIt.next());
        });
        PROCESSORS.put(IN, arg ->
            arg.addSql(arg.getCol() + " in ").addParamList());
        PROCESSORS.put(NOT_IN, arg ->
            arg.addSql(arg.getCol() + " not in ").addParamList());
        PROCESSORS.put(AND, arg -> arg.addSubsearch());
        PROCESSORS.put(OR, arg -> arg.addSubsearch());
        PROCESSORS.put(BUILDER, arg ->
            ((Consumer<ProcArg>) arg.getCriterion().getAttr()).accept(arg));
    }

    // core fields
    private final Set<Criterion> criteria = new LinkedHashSet<>();
    private Boolean distinct = Boolean.FALSE;
    private String table;
    private Integer offset;
    private Integer limit;
    private final Map<String, Object> orders = new LinkedHashMap<>();
    private final Map<String, Object> attrs = new HashMap<>();

    /**
     * clone search
     *
     * @return new search
     */
    public static Search clone(final Search origin) {
        val search = of();
        search.distinct = origin.distinct;
        search.table = origin.table;
        search.criteria.addAll(origin.criteria);
        search.orders.putAll(origin.orders);
        search.limit = origin.limit;
        search.offset = origin.offset;
        search.attrs.putAll(origin.attrs);

        return search;
    }

    /**
     * default builder
     *
     * @return search
     */
    public static Search of() {
        return new Search();
    }

    /**
     * construct by eq
     *
     * @param value id value
     * @return search
     */
    public static Search of(final Object value) {
        return new Search(value);
    }

    /**
     * construct by eq
     *
     * @param col col name
     * @param value value
     * @return search
     */
    public static Search of(final String col, final Object value) {
        return new Search(col, value);
    }

    /**
     * construct by eq
     *
     * @param field field
     * @param value value
     * @return search
     */
    public static <T, R> Search of(final FnGetter<T, R> field, final R value) {
        return of(getFieldName(field), value);
    }

    /**
     * construct by in
     *
     * @param col col name
     * @param values in values
     */
    public static Search of(final String col, final Collection<?> values) {
        return new Search(col, values);
    }

    /**
     * construct by in
     *
     * @param field field
     * @param values in values
     * @return search
     */
    public static <T, R> Search of(final FnGetter<T, R> field, final Collection<R> values) {
        return of(getFieldName(field), values);
    }

    /**
     * construct by in
     *
     * @param col col name
     * @param values in values
     * @return search
     */
    public static Search of(final String col, final Object[] values) {
        return new Search(col, values);
    }

    /**
     * construct by in
     *
     * @param field field
     * @param values in values
     * @return search
     */
    public static <T, R> Search of(final FnGetter<T, R> field, final R[] values) {
        return of(getFieldName(field), values);
    }

    /**
     * construct by builder
     *
     * @param builder criterion builder
     * @return search
     */
    public static Search of(final Consumer<ProcArg> builder) {
        return of().build(builder);
    }

    /**
     * default construct
     */
    public Search() {
    }

    /**
     * construct by id eq
     *
     * @param value id value
     */
    public Search(final Object value) {
        this(EntityUtils.ID, value);
    }

    /**
     * construct by eq
     *
     * @param col col name
     * @param value value
     */
    public Search(final String col, final Object value) {
        eq(col, value);
    }

    /**
     * construct by eq
     *
     * @param field field
     * @param value value
     */
    public <T, R> Search(final FnGetter<T, R> field, final R value) {
        this(getFieldName(field), value);
    }

    /**
     * construct by in
     *
     * @param col col name
     * @param values values
     */
    public Search(final String col, final Collection<?> values) {
        in(col, values);
    }

    /**
     * construct by in
     *
     * @param field field
     * @param values values
     */
    public <T, R> Search(final FnGetter<T, R> field, final Collection<R> values) {
        this(getFieldName(field), values);
    }

    /**
     * construct by eq
     *
     * @param col col name
     * @param values values
     */
    public Search(final String col, final Object[] values) {
        in(col, values);
    }

    /**
     * construct by in
     *
     * @param field field
     * @param values values
     */
    public <T, R> Search(final FnGetter<T, R> field, final R[] values) {
        this(getFieldName(field), values);
    }

    /**
     * set distinct
     *
     * @param distinct distinct
     * @return this
     */
    public Search distinct(final Boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /**
     * set table
     * @param table table
     * @return this
     */
    public Search table(final String table) {
        this.table = table;
        return this;
    }

    /**
     * clear criteria
     * @return search
     */
    public Search clearCriteria() {
        criteria.clear();
        return this;
    }

    /**
     * clear orders
     * @return search
     */
    public Search clearOrders() {
        orders.clear();
        return this;
    }

    /**
     * col eq
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search eq(final String col, final Object value) {
        if (value == null) {
            return isNull(col);
        }

        if (value instanceof Collection) {
            return in(col, (Collection<?>) value);
        }

        if (value.getClass().isArray()) {
            // may primitive, (Object[]) causes exception
            return in(col, arrayToList(value));
        }

        criteria.add(new Criterion(EQ, col, value));
        return this;
    }

    /**
     * field eq
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R> Search eq(final FnGetter<T, R> field, final R value) {
        return eq(getFieldName(field), value);
    }

    /**
     * col not eq value
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search ne(final String col, final Object value) {
        if (value == null) {
            return notNull(col);
        }

        if (value instanceof Collection) {
            return notIn(col, (Collection<?>) value);
        }

        if (value.getClass().isArray()) {
            // may primitive, (Object[]) causes exception
            return notIn(col, arrayToList(value));
        }

        criteria.add(new Criterion(NE, col, value));
        return this;
    }

    /**
     * field not eq value
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R> Search ne(final FnGetter<T, R> field, final R value) {
        return ne(getFieldName(field), value);
    }

    /**
     * col is null
     *
     * @param col col name
     * @return this
     */
    public Search isNull(final String col) {
        criteria.add(new Criterion(IS_NULL, col, Collections.emptyList()));
        return this;
    }

    /**
     * field is null
     *
     * @param field field
     * @return this
     */
    public <T, R> Search isNull(final FnGetter<T, R> field) {
        return isNull(getFieldName(field));
    }

    /**
     * col is not null
     *
     * @param col col
     * @return search
     */
    public Search notNull(final String col) {
        criteria.add(new Criterion(IS_NOT_NULL, col, (Object) null));
        return this;
    }

    /**
     * field is not null
     *
     * @param field field
     * @return this
     */
    public <T, R> Search notNull(final FnGetter<T, R> field) {
        return notNull(getFieldName(field));
    }

    /**
     * col is true
     *
     * @param col col name
     * @return this
     */
    public Search isTrue(final String col) {
        return eq(col, Boolean.TRUE);
    }

    /**
     * field is true
     *
     * @param field field
     * @return this
     */
    public <T, R> Search isTrue(final FnGetter<T, R> field) {
        return isTrue(getFieldName(field));
    }

    /**
     * col is false
     *
     * @param col col name
     * @return this
     */
    public Search isFalse(final String col) {
        return eq(col, Boolean.FALSE);
    }

    /**
     * field is false
     *
     * @param field field
     * @return this
     */
    public <T, R> Search isFalse(final FnGetter<T, R> field) {
        return isFalse(getFieldName(field));
    }

    /**
     * col in values array
     *
     * @param col col name
     * @param values values
     * @return this
     */
    public Search in(final String col, final Object[] values) {
        criteria.add(new Criterion(IN, col, values));
        return this;
    }

    /**
     * field in values array
     *
     * @param field field
     * @param values values
     * @return this
     */
    public <T, R> Search in(final FnGetter<T, R> field, final R[] values) {
        return in(getFieldName(field), values);
    }

    /**
     * col in values collection
     *
     * @param col col name
     * @param values values
     * @return this
     */
    public Search in(final String col, final Collection<?> values) {
        criteria.add(new Criterion(IN, col, values));
        return this;
    }

    /**
     * field in values collection
     *
     * @param field field
     * @param values values
     * @return this
     */
    public <T, R> Search in(final FnGetter<T, R> field, final Collection<R> values) {
        return in(getFieldName(field), values);
    }

    /**
     * col is not in values
     *
     * @param col col name
     * @param values values
     * @return this
     */
    public Search notIn(final String col, final Object[] values) {
        return notIn(col, Arrays.asList(values));
    }

    /**
     * field not in values
     *
     * @param field field
     * @param values values
     * @return this
     */
    public <T, R> Search notIn(final FnGetter<T, R> field, final R[] values) {
        return notIn(getFieldName(field), values);
    }

    /**
     * col is not in values
     *
     * @param col col name
     * @param values values
     * @return this
     */
    public Search notIn(final String col, final Collection<?> values) {
        criteria.add(new Criterion(NOT_IN, col, values));
        return this;
    }

    /**
     * field not in values collection
     *
     * @param field field
     * @param values values
     * @return this
     */
    public <T, R> Search notIn(final FnGetter<T, R> field, final Collection<R> values) {
        return notIn(getFieldName(field), values);
    }

    /**
     * col like value
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search like(final String col, final String value) {
        criteria.add(new Criterion(LIKE, col, value));
        return this;
    }

    /**
     * field like value
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search like(final FnGetter<T, R> field, final String value) {
        return like(getFieldName(field), value);
    }

    /**
     * col like value
     *
     * @param col col name
     * @param value value
     * @param wrapValue wrap value with %
     * @return this
     */
    public Search like(final String col, final String value, final boolean wrapValue) {
        return like(col, wrapValue ? "%" + value + "%" : value);
    }

    /**
     * field like value
     *
     * @param field field
     * @param value value
     * @param wrapValue wrap value with %
     * @return this
     */
    public <T, R extends String> Search like(final FnGetter<T, R> field, final String value, final boolean wrapValue) {
        return like(getFieldName(field), value, wrapValue);
    }

    /**
     * col not like value
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search notLike(final String col, final String value) {
        criteria.add(new Criterion(NOT_LIKE, col, value));
        return this;
    }

    /**
     * field not like value
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search notLike(final FnGetter<T, R> field, final String value) {
        return notLike(getFieldName(field), value);
    }

    /**
     * col not like value
     *
     * @param col col name
     * @param value value
     * @param wrapValue wrap value with %
     * @return this
     */
    public Search notLike(final String col, final String value, final boolean wrapValue) {
        return notLike(col, wrapValue ? "%" + value + "%" : value);
    }

    /**
     * field not like value
     *
     * @param field field
     * @param value value
     * @param wrapValue wrap value with %
     * @return this
     */
    public <T, R extends String> Search notLike(final FnGetter<T, R> field, final String value, final boolean wrapValue) {
        return notLike(getFieldName(field), value, wrapValue);
    }

    /**
     * col contains value
     *
     * @see #like
     * @param col col name
     * @param value value
     * @return this
     */
    public Search contains(final String col, final String value) {
        return like(col, value, true);
    }

    /**
     * field contains value
     *
     * @see #like
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search contains(final FnGetter<T, R> field, final String value) {
        return contains(getFieldName(field), value);
    }

    /**
     * col not contains value
     *
     * @see #notLike
     * @param col col name
     * @param value value
     * @return this
     */
    public Search notContains(final String col, final String value) {
        return notLike(col, value, true);
    }

    /**
     * field not contains value
     *
     * @see #notLike
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search notContains(final FnGetter<T, R> field, final String value) {
        return notContains(getFieldName(field), value);
    }

    /**
     * col starts with value
     *
     * @see #like
     * @param col col name
     * @param value value
     * @return this
     */
    public Search startsWith(final String col, final String value) {
        return like(col, value + "%");
    }

    /**
     * field starts with value
     *
     * @see #like
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search startsWith(final FnGetter<T, R> field, final String value) {
        return startsWith(getFieldName(field), value);
    }

    /**
     * col not starts with value
     *
     * @see #notLike
     * @param col col name
     * @param value value
     * @return this
     */
    public Search notStartsWith(final String col, final String value) {
        return notLike(col, value + "%");
    }

    /**
     * field not starts with value
     *
     * @see #notLike
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search notStartsWith(final FnGetter<T, R> field, final String value) {
        return notStartsWith(getFieldName(field), value);
    }

    /**
     * col ends with value
     *
     * @see #like(String, String)
     * @param col col name
     * @param value value
     * @return this
     */
    public Search endsWith(final String col, final String value) {
        return like(col, "%" + value);
    }

    /**
     * field ends with value
     *
     * @see #like
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search endsWith(final FnGetter<T, R> field, final String value) {
        return endsWith(getFieldName(field), value);
    }

    /**
     * col not ends with value
     *
     * @see #notLike(String, String)
     * @param col col name
     * @param value value
     * @return this
     */
    public Search notEndsWith(final String col, final String value) {
        return notLike(col, "%" + value);
    }

    /**
     * field not ends with value
     *
     * @see #like
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R extends String> Search notEndsWith(final FnGetter<T, R> field, final String value) {
        return notEndsWith(getFieldName(field), value);
    }

    /**
     * col between bottom and top
     *
     * @param col col name
     * @param bottom bottom value
     * @param top top value
     * @return this
     */
    public Search between(final String col, final Object bottom, final Object top) {
        criteria.add(new Criterion(
            BETWEEN,
            col,
            Arrays.asList(bottom, top)));
        return this;
    }

    /**
     * field between bottom and top
     *
     * @param field field
     * @param bottom bottom value
     * @param top top value
     * @return this
     */
    public <T, R> Search between(final FnGetter<T, R> field, final R bottom, final R top) {
        return between(getFieldName(field), bottom, top);
    }

    /**
     * col not between bottom and top
     *
     * @param col col name
     * @param bottom bottom value
     * @param top top value
     * @return this
     */
    public Search notBetween(final String col, final Object bottom, final Object top) {
        criteria.add(new Criterion(
            NOT_BETWEEN,
            col,
            Arrays.asList(bottom, top)));
        return this;
    }

    /**
     * field not between bottom and top
     *
     * @param field field
     * @param bottom bottom value
     * @param top top value
     * @return this
     */
    public <T, R> Search notBetween(final FnGetter<T, R> field, final R bottom, final R top) {
        return notBetween(getFieldName(field), bottom, top);
    }

    /**
     * col is less than value
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search lt(final String col, final Object value) {
        criteria.add(new Criterion(LT, col, value));
        return this;
    }

    /**
     * field is less than value
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R> Search lt(final FnGetter<T, R> field, final R value) {
        return lt(getFieldName(field), value);
    }

    /**
     * col is less than value or equals value
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search lte(final String col, final Object value) {
        criteria.add(new Criterion(LTE, col, value));
        return this;
    }

    /**
     * field is less than value or equals value
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R> Search lte(final FnGetter<T, R> field, final R value) {
        return lte(getFieldName(field), value);
    }

    /**
     * col is greater than value
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search gt(final String col, final Object value) {
        criteria.add(new Criterion(GT, col, value));
        return this;
    }

    /**
     * field is greater than value
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R> Search gt(final FnGetter<T, R> field, final R value) {
        return gt(getFieldName(field), value);
    }

    /**
     * col is greater than value or equals value
     *
     * @param col col name
     * @param value value
     * @return this
     */
    public Search gte(final String col, final Object value) {
        criteria.add(new Criterion(GTE, col, value));
        return this;
    }

    /**
     * col is greater than value or equals value
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R> Search gte(final FnGetter<T, R> field, final R value) {
        return gte(getFieldName(field), value);
    }

    /**
     * and another search
     *
     * @param search search
     * @return this
     */
    public Search and(final Search search, final Search ... searches) {
        if (StrUtils.isBlank(search.table)) {
            search.table = table;
        }

        criteria.add(new Criterion(AND, search));
        eachSearch(searches, this::and);

        return this;
    }

    /**
     * or col eq val
     *
     * @param col col
     * @param value value
     * @return this
     */
    public Search or(final String col, final Object value) {
        return or(new Search(col, value).table(table));
    }

    /**
     * or field eq val
     *
     * @param field field
     * @param value value
     * @return this
     */
    public <T, R> Search or(final FnGetter<T, R> field, final R value) {
        return or(getFieldName(field), value);
    }

    /**
     * or another search
     *
     * @param search search
     * @return this
     */
    public Search or(final Search search, final Search ... searches) {
        if (StrUtils.isBlank(search.table)) {
            search.table = table;
        }

        criteria.add(new Criterion(OR, search));
        eachSearch(searches, this::or);

        return this;
    }

    void eachSearch(final Search[] searches, final Consumer<Search> consumer) {
        if (searches != null && searches.length > 0) {
            for (val search : searches) {
                consumer.accept(search);
            }
        }
    }

    /**
     * build custom search criterion
     *
     * @param builder criterion builder
     * @return this
     */
    public Search build(final Consumer<ProcArg> builder) {
        criteria.add(new Criterion(BUILDER, builder));
        return this;
    }

    /**
     * order by col asc
     *
     * @param col col
     * @return this
     */
    public Search asc(final String col) {
        return orderBy(col, Order.ASC);
    }

    /**
     * order by field asc
     *
     * @param field field
     * @return this
     */
    public <T, R> Search asc(final FnGetter<T, R> field) {
        return asc(getFieldName(field));
    }

    /**
     * order by col desc
     *
     * @param col col name
     * @return this
     */
    public Search desc(final String col) {
        return orderBy(col, Order.DESC);
    }

    /**
     * order by field desc
     *
     * @param field field
     * @return this
     */
    public <T, R> Search desc(final FnGetter<T, R> field) {
        return desc(getFieldName(field));
    }

    /**
     * order by
     *
     * @param col col name
     * @param order order
     * @return this
     */
    public Search orderBy(final String col, final Order order) {
        orders.put(col, order.name());
        return this;
    }

    /**
     * order by
     *
     * @param field field
     * @param order order
     * @return this
     */
    public <T, R> Search orderBy(final FnGetter<T, R> field, final Order order) {
        return orderBy(getFieldName(field), order);
    }

    /**
     * order by values, MySQL only
     *
     * @param col col name
     * @param values values
     * @return this
     */
    public Search orderBy(final String col, final Collection<?> values) {
        orders.put(col, values);
        return this;
    }

    /**
     * order by values, MySQL only
     *
     * @param field field
     * @param values values
     * @return this
     */
    public <T, R> Search orderBy(final FnGetter<T, R> field, final Collection<R> values) {
        return orderBy(getFieldName(field), values);
    }

    /**
     * set offset
     *
     * @param offset offset
     * @return this
     */
    public Search offset(final Integer offset) {
        this.offset = offset;
        return this;
    }

    /**
     * get offset
     *
     * @return offset
     */
    public Integer offset() {
        return offset;
    }

    /**
     * set limit
     *
     * @param limit limit
     * @return this
     */
    public Search limit(final Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * get limit
     *
     * @return limit
     */
    public Integer limit() {
        return limit;
    }

    /**
     * @return orders
     */
    public Map<String, Object> orders() {

        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }

        val ordersRtn = new LinkedHashMap<String, Object>();
        orders.forEach((k, v) -> ordersRtn.put(ProcArg.col(table, k), v));
        return ordersRtn;
    }

    /**
     * assemble search to sql and param list
     *
     * @return sql and param list
     */
    public List<Object> assemble() {
        return assemble(false);
    }

    /**
     * @return true if distinct
     */
    public boolean hasDistinct() {
        return distinct;
    }

    /**
     * @return true if has criterion
     */
    public boolean hasCriterion() {
        return !criteria.isEmpty();
    }

    /**
     * if search has criterion of col
     *
     * @param col col
     * @return true if has col criterion
     */
    public boolean hasCriterion(final String col) {
        for (val criterion : criteria) {
            if (col.equals(criterion.getCol())) {
                return true;
            }
        }
        return false;
    }

    /**
     * if search has criterion of col prefix
     *
     * @param colPrefix col prefix
     * @return true if has col criterion of prefix
     */
    public boolean hasCriterionPrefix(final String colPrefix) {
        for (val criterion : criteria) {
            if (criterion.getCol().startsWith(colPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if has no criterion
     */
    public boolean hasNoCriterion() {
        return criteria.isEmpty();
    }

    /**
     * @return true if has order
     */
    public boolean hasOrder() {
        return !orders.isEmpty();
    }

    /**
     * @return true if has no order
     */
    public boolean hasNoOrder() {
        return orders.isEmpty();
    }

    /**
     * check attrs data
     * @param name name
     * @return true if has attr
     */
    public boolean hasAttr(final String name) {
        return attrs.containsKey(name);
    }

    /**
     * check attr val is true
     * @param name attr name
     * @return true if attr is true
     */
    public boolean trueAttr(final String name) {
        val val = attr(name);
        return (val instanceof Boolean) && (Boolean) val;
    }

    /**
     * get attrs data
     * @param name name
     * @param <T> data type
     * @return data
     */
    public <T> T getAttr(final String name) {
        return attr(name);
    }

    /**
     * get attrs data
     * @param name name
     * @param <T> data type
     * @return data
     */
    @SuppressWarnings("unchecked")
    public <T> T attr(final String name) {
        return (T) attrs.get(name);
    }

    /**
     * set attr
     * @param name name
     * @param value value
     * @return this
     */
    public Search setAttr(final String name, final Object value) {
        return attr(name, value);
    }

    /**
     * set attr
     * @param name name
     * @param value value
     * @return this
     */
    public Search attr(final String name, final Object value) {
        attrs.put(name, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return assemble().toString();
    }

    // --
    // private methods

    /**
     * get table
     * @return table
     */
    String table() {
        return this.table;
    }

    List<Object> assemble(final boolean subSearch) {
        val result = new LinkedList<>();
        val arg = new ProcArg(table, result);

        for (val criterion : criteria) {
            arg.setCriterion(criterion);
            val type = criterion.getType();

            if (type == OR) {
                if (arg.isHasPrevCol()) {
                    arg.addSql(" or ");
                }
                else {
                    arg.setHasPrevOrCol(true);
                }
            }
            // and
            else {
                if (arg.isHasPrevOrCol()) {
                    arg.addSql(" or ");
                    arg.setHasPrevOrCol(false);
                }
                else if (arg.isHasPrevCol()) {
                    arg.addSql(" and ");
                }
            }

            PROCESSORS.get(type).accept(arg);

            if (!arg.isHasPrevCol()) {
                arg.setHasPrevCol(true);
            }
        }

        if (subSearch && criteria.size() > 1) {
            result.add(0, SqlParam.rawVal("("));
            result.add(SqlParam.rawVal(")"));
        }

        return result;
    }

    List<Object> arrayToList(final Object array) {
        val length = Array.getLength(array);
        val list = new ArrayList<>(length);
        for (int i = 0; i < length; ++i) {
            list.add(Array.get(array, i));
        }
        return list;
    }
}
