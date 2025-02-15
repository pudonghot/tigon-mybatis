package com.pudonghot.tigon.mybatis;

import lombok.val;
import java.util.List;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.ibatis.annotations.Param;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import com.pudonghot.tigon.mybatis.xmlgen.annotation.MapperXmlEl;
import static com.pudonghot.tigon.mybatis.util.FnGetterUtils.getFieldName;

/**
 * @author Donghuang
 * @date Oct 17, 2015 2:09:20 PM
 */
@SuppressWarnings("hiding")
@MapperXmlEl(tag = MapperXmlEl.Tag.SELECT, id = "count", resultType = "int")
@MapperXmlEl(tag = MapperXmlEl.Tag.SELECT, id = "exists", resultType = "boolean")
@MapperXmlEl(tag = MapperXmlEl.Tag.SELECT, id = "find", resultType = MapperXmlEl.RESULT_TYPE_ENTITY)
@MapperXmlEl(tag = MapperXmlEl.Tag.SELECT, id = "findCol", resultType = "object", include = "Tigon.selectCol")
@MapperXmlEl(tag = MapperXmlEl.Tag.SELECT, id = "list", resultType = MapperXmlEl.RESULT_TYPE_ENTITY)
@MapperXmlEl(tag = MapperXmlEl.Tag.SELECT, id = "listCol", resultType = "object", include = "Tigon.selectCol")
public interface BaseQueryMapper<PrimaryKey, Entity> extends SuperMapper<Entity> {

    /**
     * count by search
     *
     * @param search search
     * @return count
     */
    int count(@Param(PARAM_SEARCH_KEY) Search search);

    /**
     * find one by search
     *
     * @param search search
     * @return true if exists rows
     */
    boolean exists(@Param(PARAM_SEARCH_KEY) Search search);

    /**
     * find one by search
     *
     * @param search search
     * @return find result or null
     */
    Entity find(@Param(PARAM_SEARCH_KEY) Search search);

    /**
     * find one by PrimaryKey
     *
     * @param primaryKey primaryKey
     * @return find result or null
     */
    Entity find(@Param(PARAM_SEARCH_KEY) PrimaryKey primaryKey);

    /**
     * find col by search
     *
     * @param col select col
     * @param search search
     * @return col result
     */
    <T> T findCol(@Param(PARAM_COL_KEY) String col, @Param(PARAM_SEARCH_KEY) Search search);

    /**
     * find col by search
     *
     * @param field select col
     * @param search search
     * @return col result
     */
    default <T, R> R findCol(final FnGetter<T, R> field, final Search search) {
        return findCol(getFieldName(field), search);
    }

    /**
     * list by search
     *
     * @param search search
     * @return list result or empty list
     */
    List<Entity> list(@Param(PARAM_SEARCH_KEY) Search search);

    /**
     * list by primary keys
     *
     * @param keys primary keys
     * @return list result or empty list
     */
    default List<Entity> list(final Collection<PrimaryKey> keys) {
        return list(new Search(keys));
    }

    /**
     * list by primary keys
     *
     * @param keys primary keys
     * @return list result or empty list
     */
    default List<Entity> list(final PrimaryKey[] keys) {
        return list(new Search(keys));
    }

    /**
     * list col by search
     *
     * @param col    select col
     * @param search search
     * @return list result or empty list
     */
    <T> List<T> listCol(@Param(PARAM_COL_KEY) String col, @Param(PARAM_SEARCH_KEY) Search search);

    /**
     * list col by search
     *
     * @param field select col
     * @param search search
     * @return col result
     */
    default <T, R> List<R> listCol(final FnGetter<T, R> field, final Search search) {
        return listCol(getFieldName(field), search);
    }

    /**
     * scan entities
     *
     * @param pageSize page size
     * @param search search
     * @param scanner scanner
     * @return false if no data found
     */
    default boolean scan(final int pageSize,
                      final Search search,
                      final Consumer<Entity> scanner) {
        return batchScan(pageSize, search, list -> list.forEach(scanner::accept));
    }

    /**
     * scan entities
     *
     * @param pageSize page size
     * @param search search
     * @param scanner scanner
     * @return false if no data found
     */
    default boolean batchScan(final int pageSize,
                           final Search search,
                           final Consumer<List<Entity>> scanner) {
        return batchScan(pageSize, search, this::count, this::list, scanner);
    }

    /**
     * scan entities
     *
     * @param pageSize page size
     * @param search search
     * @param countMethod count method
     * @param listMethod list method
     * @param scanner scanner
     * @return false if no data found
     */
    default <T> boolean batchScan(
            final int pageSize,
            final Search search,
            final Function<Search, Integer> countMethod,
            final Function<Search, List<T>> listMethod,
            final Consumer<List<T>> scanner) {

        val total = countMethod.apply(search);

        if (total > 0) {
            for (int start = 0; start < total; start += pageSize) {
                scanner.accept(listMethod.apply(search.offset(start).limit(Math.min(pageSize, total - start))));
            }
            return true;
        }

        return false;
    }
}
