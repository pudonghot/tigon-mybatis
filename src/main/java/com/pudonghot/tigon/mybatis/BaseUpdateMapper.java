package com.pudonghot.tigon.mybatis;

import java.util.Map;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import com.pudonghot.tigon.mybatis.util.FnGetterUtils;
import com.pudonghot.tigon.mybatis.xmlgen.annotation.MapperXmlEl;
import static com.pudonghot.tigon.mybatis.util.FnGetterUtils.getFieldName;

/**
 * @author Donghuang
 * @date Oct 17, 2015 2:09:20 PM
 */
@SuppressWarnings("hiding")
@MapperXmlEl(tag = MapperXmlEl.Tag.UPDATE, id = "update")
@MapperXmlEl(tag = MapperXmlEl.Tag.UPDATE, id = "setNull")
public interface BaseUpdateMapper<PrimaryKey, Entity> extends SuperMapper<Entity> {

    /**
     * update entity by primary key
     *
     * @param entity update entity
     * @return update result
     */
    int update(@Param(PARAM_MODEL_KEY) Entity entity);

    /**
     * update entities by primary key
     *
     * @param entities update entities
     * @return update result
     */
    int update(@Param(PARAM_MODELS_KEY) Collection<Entity> entities);

    /**
     * update by search
     *
     * @param entity update entity
     * @param search update search
     * @return update result
     */
    int update(@Param(PARAM_MODEL_KEY) Entity entity, @Param(PARAM_SEARCH_KEY) Search search);

    /**
     * update by search
     *
     * @param update update map
     * @param search update search
     * @return update result
     */
    int update(@Param(PARAM_MODEL_KEY) Map<String, ?> update, @Param(PARAM_SEARCH_KEY) Search search);

    /**
     * update by primary key
     *
     * @param update update model
     * @param primaryKey primary key
     * @return update result
     */
    int update(@Param(PARAM_MODEL_KEY) Map<String, ?> update, @Param(PARAM_SEARCH_KEY) PrimaryKey primaryKey);

    /**
     * update col val
     *
     * @param col col
     * @param val val
     * @param search search
     * @return update result
     */
    int update(@Param(PARAM_COL_KEY) String col, @Param(PARAM_VAL_KEY) Object val, @Param(PARAM_SEARCH_KEY) Search search);

    /**
     * update col val
     *
     * @param field field
     * @param val val
     * @param search search
     * @return update result
     */
    default <T, R> int update(final FnGetter<T, R> field, final R val, final Search search) {
        return update(getFieldName(field), val, search);
    }

    /**
     * update col val
     *
     * @param col col
     * @param val val
     * @param primaryKey primary key
     * @return update result
     */
    int update(@Param(PARAM_COL_KEY) String col, @Param(PARAM_VAL_KEY) Object val, @Param(PARAM_SEARCH_KEY) PrimaryKey primaryKey);

    /**
     * update col val
     *
     * @param field field
     * @param val val
     * @param primaryKey primary key
     * @return update result
     */
    default <T, R> int update(final FnGetter<T, R> field, final R val, final PrimaryKey primaryKey) {
        return update(getFieldName(field), val, primaryKey);
    }

    /**
     * set col null
     *
     * @param col col
     * @param search search
     * @return update result
     */
    int setNull(@Param(PARAM_COL_KEY) String col, @Param(PARAM_SEARCH_KEY) Search search);

    /**
     * set col to null
     *
     * @param field field
     * @param search search
     * @return update result
     */
    default <T, R> int setNull(final FnGetter<T, R> field, final Search search) {
        return setNull(getFieldName(field), search);
    }

    /**
     * set cols null
     *
     * @param cols cols
     * @param search search
     * @return update result
     */
    int setNull(@Param(PARAM_COLS_KEY) String[] cols, @Param(PARAM_SEARCH_KEY) Search search);

    /**
     * set cols to null
     *
     * @param fields fields
     * @param search search
     * @return update result
     */
    default <T> int setNull(final Collection<FnGetter<T, ?>> fields, final Search search) {
        return setNull(fields.stream().map(FnGetterUtils::getFieldName).toArray(String[]::new), search);
    }

}
